# Lokal utvikling

## Demo-livssyklus (anbefalt)

Bruk `scripts/demo.sh` for å starte og stoppe hele demoen deklarativt.
Skriptet venter på at PostgreSQL og Keycloak faktisk er healthy før appen
startes, og garanterer at Flyway kjører mot riktig database ved restart —
dette unngår "run i sirkler"-feilsøking.

```bash
scripts/demo.sh up        # start Compose + app, vent til alt svarer
scripts/demo.sh status    # se nåværende tilstand (Compose/Keycloak/app)
scripts/demo.sh logs      # følg app-loggen
scripts/demo.sh restart   # garantert ren restart (fikser "tom database"-feilen)
scripts/demo.sh down      # stopp app + Compose, behold data
scripts/demo.sh wipe      # stopp alt og slett Compose-volumer (nullstill demo)
```

`up` er idempotent: kjør den trygt flere ganger. Den leser `.env` automatisk
hvis filen finnes (Keycloak service-konto), og advarer tydelig hvis den
mangler i stedet for å feile stille.

Manuell fremgangsmåte (samme steg som skriptet automatiserer):

```bash
docker compose up -d
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
Søknadssiden er server-rendered og skal derfor ikke vise Vaadins
development-mode-klient.

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

### Lokal ende-til-ende-test

Den lokale service-kontoen konfigureres med en tilfeldig klienthemmelighet.
Lagre den bare i den git-ignorerte `.env`-filen med eiertilgang. `scripts/demo.sh up`
leser den automatisk; restart appen med `scripts/demo.sh restart` etter at
`.env` er oppdatert.

Etter restart kan `buyer-demo` sende en søknad og `admin-demo` godkjenne den.
Logg deretter ut og inn igjen som samme identitet for at den nye
`SELLER`-rollen skal være med i OIDC-sesjonen. Ikke legg `.env` eller
klienthemmeligheten i repository.

## Kjente fallgruver

- **Grå «Vaadin dev mode»-skjerm med feiltekst etter innlogging**: Dette er
  ikke en kompileringsfeil i appen, men Vaadins innebygde AI-utviklerverktøy
  «Vaadin Copilot» (nytt i Vaadin 25, uavhengig av GitHub Copilot). Det
  fanger opp JavaScript-feil i nettleseren og kan vise en fullskjerms grå
  overlay over appen. Det er deaktivert i denne demoen via
  `vaadin.copilot.enable=false` i `application.properties`. Hvis skjermen
  likevel dukker opp: sjekk at appen er startet på nytt etter siste
  `git pull`/endring (`scripts/demo.sh restart`), og at nettleseren ikke
  har en gammel fane åpen fra før konfigurasjonen ble lagt til.

- **IntelliJ viser feil som «`org.springframework.transaction.annotation`
  does not exist»**: Dette er ikke en reell kompileringsfeil (`./mvnw
  compile`/`verify` bekrefter dette gjentatte ganger), men et tegn på at
  IntelliJ aldri har importert prosjektet som et Maven-modul (ingen
  `.iml`-fil i `.idea/`). Løsning: åpne Maven-verktøyvinduet i IntelliJ og
  trykk «Reload All Maven Projects» (eller høyreklikk `pom.xml` → Add as
  Maven Project). Du trenger ikke kjøre appen fra IntelliJ for å teste
  demoen — `scripts/demo.sh` bygger og starter appen med riktig,
  verifisert classpath.

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

Stopp lokal demo uten å slette data:

```bash
scripts/demo.sh down
```
