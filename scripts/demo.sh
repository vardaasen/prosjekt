#!/usr/bin/env bash
#
# Deklarativ styring av den lokale demoen: Compose (PostgreSQL + Keycloak)
# og Spring Boot-appen som én enhet, med helsesjekker og korrekt
# rekkefølge, slik at Flyway alltid kjører mot riktig database.
#
# Bruk:
#   scripts/demo.sh up        # start compose + app (idempotent)
#   scripts/demo.sh down      # stopp app + compose (beholder data)
#   scripts/demo.sh restart   # garantert ren restart (fikser "tom database")
#   scripts/demo.sh status    # vis nåværende tilstand
#   scripts/demo.sh logs      # følg app-loggen
#   scripts/demo.sh wipe      # stopp alt og slett Compose-volumer
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RUN_DIR="$ROOT_DIR/.demo"
PID_FILE="$RUN_DIR/app.pid"
LOG_FILE="$RUN_DIR/app.log"
APP_URL="http://localhost:8080"
KEYCLOAK_URL="http://localhost:8180"
KEYCLOAK_REALM="havbruksbrukt"

mkdir -p "$RUN_DIR"

log() { printf '\033[1;34m[demo]\033[0m %s\n' "$1"; }
warn() { printf '\033[1;33m[demo]\033[0m %s\n' "$1"; }
fail() { printf '\033[1;31m[demo]\033[0m %s\n' "$1" >&2; exit 1; }

http_ok() {
    curl --silent --show-error --output /dev/null --max-time 5 --write-out '%{http_code}' "$1" 2>/dev/null | grep -q "$2"
}

wait_for() {
    local description="$1" check="$2" attempts="${3:-30}" delay="${4:-2}"
    for ((i = 1; i <= attempts; i++)); do
        if eval "$check"; then
            return 0
        fi
        sleep "$delay"
    done
    warn "Tidsavbrudd mens vi ventet på: $description"
    return 1
}

app_pid_alive() {
    [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null
}

compose_healthy() {
    local service="$1"
    local state
    state="$(docker compose ps --format json "$service" 2>/dev/null | grep -o '"Health":"[a-z]*"' | head -n1)"
    [[ "$state" == '"Health":"healthy"' ]]
}

start_compose() {
    ensure_database_config
    log "Starter PostgreSQL og Keycloak via Compose ..."
    docker compose up -d

    wait_for "PostgreSQL (marketplace) er healthy" \
        'compose_healthy postgres' || fail "PostgreSQL ble aldri healthy. Kjør 'docker compose logs postgres'."
    log "PostgreSQL er klar."

    wait_for "Keycloak svarer på realm '$KEYCLOAK_REALM'" \
        "http_ok '$KEYCLOAK_URL/realms/$KEYCLOAK_REALM' '200'" 60 2 \
        || fail "Keycloak svarte aldri. Kjør 'docker compose logs keycloak'."
    log "Keycloak er klar på $KEYCLOAK_URL."
}

stop_app() {
    if app_pid_alive; then
        local pid
        pid="$(cat "$PID_FILE")"
        log "Stopper kjørende app (PID $pid) ..."
        kill "$pid" 2>/dev/null || true
        wait_for "app-prosessen avsluttet" "! kill -0 $pid 2>/dev/null" 20 1 || {
            warn "Prosessen svarte ikke på SIGTERM, sender SIGKILL."
            kill -9 "$pid" 2>/dev/null || true
        }
    else
        # Fang opp en app startet utenfor dette skriptet, f.eks. manuelt.
        local existing_pid
        existing_pid="$(lsof -nP -iTCP:8080 -sTCP:LISTEN -t 2>/dev/null | head -n1 || true)"
        if [[ -n "$existing_pid" ]]; then
            log "Fant app på port 8080 utenfor skriptets kontroll (PID $existing_pid), stopper den."
            kill "$existing_pid" 2>/dev/null || true
            wait_for "app-prosessen avsluttet" "! kill -0 $existing_pid 2>/dev/null" 20 1 || true
        fi
    fi
    rm -f "$PID_FILE"
}

# Realmfilen deklarerer marketplace-provisioner uten fast hemmelighet (ingen
# hemmeligheter i Git), så Keycloak genererer en ny tilfeldig hemmelighet ved
# hver realm-import, f.eks. etter 'wipe'. Hent derfor gjeldende hemmelighet fra
# Keycloak og skriv den til den git-ignorerte .env før appen starter, slik at
# appen og Keycloak alltid er enige. Uten dette gir godkjenning av
# selgersøknader 401 "Invalid client credentials".
sync_provisioner_secret() {
    local secret
    # kcadm kjører inne i Keycloak-containeren med containerens egne
    # bootstrap-admin-variabler; skriptet trenger ikke kjenne passordet.
    secret="$(docker compose exec -T keycloak bash -c '
        set -e
        kcadm=/opt/keycloak/bin/kcadm.sh
        config=/tmp/kcadm-demo.config
        $kcadm config credentials --config "$config" --server http://localhost:8080 \
            --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" \
            --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >/dev/null 2>&1
        id="$($kcadm get clients --config "$config" -r '"$KEYCLOAK_REALM"' \
            -q clientId=marketplace-provisioner --fields id --format csv --noquotes)"
        [ -n "$id" ] || exit 3
        $kcadm get "clients/$id/client-secret" --config "$config" -r '"$KEYCLOAK_REALM"' \
            --fields value --format csv --noquotes
    ')" || {
        warn "Fant ikke hemmeligheten til marketplace-provisioner i Keycloak. \
Godkjenning av selgersøknader vil feile. Finnes realmet fra før uten klienten, \
kjør 'scripts/demo.sh wipe && scripts/demo.sh up' for å importere realmet på nytt."
        return 0
    }
    secret="$(printf '%s' "$secret" | tr -d '\r\n')"
    [[ -n "$secret" ]] || { warn "Keycloak returnerte en tom klienthemmelighet."; return 0; }

    (umask 077 && touch .env)
    set_env_value KEYCLOAK_ADMIN_ENABLED true
    set_env_value KEYCLOAK_ADMIN_BASE_URL "$KEYCLOAK_URL"
    set_env_value KEYCLOAK_ADMIN_REALM "$KEYCLOAK_REALM"
    set_env_value KEYCLOAK_ADMIN_CLIENT_ID marketplace-provisioner
    set_env_value KEYCLOAK_ADMIN_CLIENT_SECRET "$secret"
    log "Synkroniserte hemmeligheten til marketplace-provisioner fra Keycloak til .env."
}

ensure_database_config() {
    # Ensure .env exists with database configuration for local development.
    # These values match the default docker compose configuration.
    (umask 077 && touch .env)
    
    # Only set if not already present
    if ! grep -q '^DATABASE_URL=' .env 2>/dev/null; then
        set_env_value DATABASE_URL "jdbc:postgresql://localhost:5432/havbruksbrukt"
    fi
    if ! grep -q '^DATABASE_USERNAME=' .env 2>/dev/null; then
        set_env_value DATABASE_USERNAME "havbruksbrukt"
    fi
    if ! grep -q '^DATABASE_PASSWORD=' .env 2>/dev/null; then
        set_env_value DATABASE_PASSWORD "havbruksbrukt"
    fi
    if ! grep -q '^POSTGRES_DB=' .env 2>/dev/null; then
        set_env_value POSTGRES_DB "havbruksbrukt"
    fi
    if ! grep -q '^POSTGRES_USER=' .env 2>/dev/null; then
        set_env_value POSTGRES_USER "havbruksbrukt"
    fi
    if ! grep -q '^POSTGRES_PASSWORD=' .env 2>/dev/null; then
        set_env_value POSTGRES_PASSWORD "havbruksbrukt"
    fi
}

set_env_value() {
    local key="$1" value="$2" tmp
    tmp="$(mktemp "$RUN_DIR/env.XXXXXX")"
    awk -v key="$key" -v value="$value" '
        index($0, key "=") == 1 { print key "=" value; found = 1; next }
        { print }
        END { if (!found) print key "=" value }
    ' .env > "$tmp"
    chmod 600 "$tmp"
    mv "$tmp" .env
}

start_app() {
    if app_pid_alive; then
        log "App kjører allerede (PID $(cat "$PID_FILE"))."
        return 0
    fi

    if http_ok "$APP_URL/selg" '200'; then
        log "App svarer allerede på $APP_URL (startet utenfor scripts/demo.sh). Bruker den som den er."
        warn "Merk: denne prosessen er ikke sporet av scripts/demo.sh (ingen PID-fil). \
Bruk 'scripts/demo.sh restart' for full kontroll neste gang."
        return 0
    fi

    ensure_database_config
    sync_provisioner_secret

    if [[ -f .env ]]; then
        set -a
        # shellcheck disable=SC1091
        source .env
        set +a
        log "Lastet .env (database og Keycloak service-konto)."
    else
        warn ".env finnes ikke. Appen vil feile uten DATABASE_* miljøvariabler."
        return 1
    fi

    log "Bygger og starter appen (logg: $LOG_FILE) ..."
    nohup ./mvnw spring-boot:run >"$LOG_FILE" 2>&1 &
    local pid=$!
    disown "$pid" 2>/dev/null || true
    echo "$pid" > "$PID_FILE"

    wait_for "appen svarer på $APP_URL/selg" \
        "http_ok '$APP_URL/selg' '200'" 90 2 || {
        warn "Appen ble aldri klar. Siste loggljner:"
        tail -n 40 "$LOG_FILE" >&2 || true
        fail "Se full logg med: scripts/demo.sh logs"
    }
    log "Appen svarer 200 på $APP_URL/selg."
}

print_status() {
    log "Compose-tjenester:"
    docker compose ps 2>/dev/null || warn "Compose kjører ikke."

    if compose_healthy postgres; then
        log "PostgreSQL: healthy"
    else
        warn "PostgreSQL: ikke healthy/kjører ikke"
    fi

    if http_ok "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM" '200'; then
        log "Keycloak: svarer på $KEYCLOAK_URL"
    else
        warn "Keycloak: svarer ikke på $KEYCLOAK_URL"
    fi

    if http_ok "$APP_URL/selg" '200'; then
        if app_pid_alive; then
            log "App: kjører (PID $(cat "$PID_FILE")) og svarer 200 på $APP_URL"
        else
            log "App: svarer 200 på $APP_URL (startet utenfor scripts/demo.sh, ingen PID-fil)"
        fi
    elif app_pid_alive; then
        warn "App-prosess kjører (PID $(cat "$PID_FILE")), men svarer ikke ennå. Se 'scripts/demo.sh logs'."
    else
        warn "App: kjører ikke"
    fi
}

cmd_up() {
    start_compose
    start_app
    print_status
    cat <<EOF

Demo klar:
  Katalog      $APP_URL/utstyr
  Selgerinngang $APP_URL/selg
  Keycloak     $KEYCLOAK_URL

Demoidentiteter: se docs/local-development.md
EOF
}

cmd_down() {
    stop_app
    log "Stopper Compose (data beholdes) ..."
    docker compose down
}

cmd_restart() {
    # Garanterer at Flyway alltid kjører mot riktig database ved å alltid
    # stoppe appen før Compose restartes, uansett tidligere tilstand.
    stop_app
    docker compose down
    start_compose
    start_app
    print_status
}

cmd_wipe() {
    stop_app
    warn "Sletter Compose-volumer (all lokal data går tapt) ..."
    docker compose down --volumes
}

cmd_logs() {
    [[ -f "$LOG_FILE" ]] || fail "Ingen logg funnet. Kjør 'scripts/demo.sh up' først."
    tail -n 200 -f "$LOG_FILE"
}

case "${1:-}" in
    up) cmd_up ;;
    down) cmd_down ;;
    restart) cmd_restart ;;
    status) print_status ;;
    logs) cmd_logs ;;
    wipe) cmd_wipe ;;
    *)
        cat <<EOF
Bruk: scripts/demo.sh <kommando>

  up       Start Compose + app (idempotent, venter på helse)
  down     Stopp app + Compose (beholder data)
  restart  Garantert ren restart (fikser "relation does not exist")
  status   Vis nåværende tilstand
  logs     Følg app-loggen
  wipe     Stopp alt og slett Compose-volumer
EOF
        exit 1
        ;;
esac
