# Lokal utvikling

## Markedsplass med Keycloak

Start lokal PostgreSQL og Keycloak:

```bash
docker compose up -d
```

Vent til Keycloak er klar på `http://localhost:8180`, og start deretter
markedsplassen:

```bash
./mvnw spring-boot:run
```

`http://localhost:8080/utstyr` er offentlig. `http://localhost:8080/app`
videresender anonyme brukere til Keycloak. Logg inn med den lokale
demo-selgeren:

| Felt | Verdi |
| --- | --- |
| Brukernavn | `seller-demo` |
| Passord | `seller-demo-password` |

`buyer-demo` har bare rollen `BUYER` og får ikke tilgang til `/app`. Disse
brukerne og administratorpassordets Compose-standardverdi er kun for lokal
utvikling. De må ikke brukes eller importeres til staging eller produksjon.

Den offentlige inngangen `http://localhost:8080/selg` forklarer
selgerreisen og leder en eksisterende bruker til innlogging og
`/selgersoknad`. Keycloaks selvregistrering er med hensikt ikke aktivert i
den lokale realm-importen: uten e-postleveranse/verifisering og rate limiting
skal det ikke være mulig å opprette vilkårlige kontoer. Logg inn som
`buyer-demo` for å teste selgersøknaden.

Den lokale markedsplassadministratoren er separat fra Keycloaks
bootstrap-administrator:

| Felt | Verdi |
| --- | --- |
| Brukernavn | `admin-demo` |
| Passord | `admin-demo-password` |
| Rolle | `ADMIN` |
| Appflate | `http://localhost:8080/admin` |

`admin-demo` er bare for å teste tilgangsgrensen og
godkjenningsskjermen. Den kan ikke brukes som Keycloak-plattformadministrator.

`buyer-demo` kan sende en selgersøknad på
`http://localhost:8080/selgersoknad`. Godkjenning stopper med en synlig feil
inntil en Keycloak service-konto er konfigurert; det hindrer at databasen
markerer søknaden som godkjent uten at `SELLER` faktisk er tildelt.

Prøv `seller-demo` på `/admin` for å verifisere at manglende administratorrolle
viser den brukervennlige 403-siden. Ved avbrutt OIDC-innlogging sendes brukeren
til `/innlogging-feilet`, uten tekniske feildetaljer.

## Keycloak-rolletildeling per miljø

Før en administrator kan godkjenne søknader i et miljø, må drift opprette en
egen konfidensiell Keycloak-klient med service account. Kontoen skal bare få
rettighetene som trengs for å finne en bruker og tildele realmrollen `SELLER`
(`view-users`, `manage-users` og `view-realm` i `realm-management`).

Aktiver adapteren med hemmeligheter fra miljøets hemmelighetsforvaltning,
aldri fra repository eller `application.properties`:

```bash
export KEYCLOAK_ADMIN_ENABLED=true
export KEYCLOAK_ADMIN_BASE_URL=https://keycloak.example.no
export KEYCLOAK_ADMIN_REALM=havbruksbrukt
export KEYCLOAK_ADMIN_CLIENT_ID=marketplace-provisioner
export KEYCLOAK_ADMIN_CLIENT_SECRET='fra-hemmelighetsforvaltning'
```

Adapteren bruker client-credentials, slår opp OIDC-subject som Keycloaks
stabile bruker-ID, og tildeler bare `SELLER`. Den oppretter aldri `ADMIN`.

## Produksjonsgrenser

Compose-filen kjører Keycloak med `start-dev` og en lokal, ukryptert
utviklingsdatabase. Produksjon må kjøre Keycloak separat med HTTPS, sikker
hemmelighetsforvaltning, særskilt realm/klient, administratorkonto og
produksjonsdatabase. Overstyr OIDC-endepunktene gjennom de dokumenterte
`KEYCLOAK_*`-miljøvariablene; aldri legg produksjonsverdier i
`application.properties`.

Følg userflow 8, `docs/userflows/diagrams/08-operator-installs-and-bootstraps-environment.mmd`,
ved oppstart av staging eller produksjon. Første markedsplassadministrator
opprettes manuelt etter Keycloak-deploy og før markedsplassen eksponeres for
registrering; applikasjonen skal aldri opprette eller tildele `ADMIN` ved
oppstart.

Stopp lokale containere uten å slette data:

```bash
docker compose down
```
