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

Før en administrator kan godkjenne søknader i et miljø, må Keycloak ha en
egen konfidensiell klient med service account som bare får rettighetene som
trengs for å finne en bruker og tildele realmrollen `SELLER` (`view-users`,
`manage-users` og `view-realm` i `realm-management`). I lokal utvikling er
denne klienten (`marketplace-provisioner`) og rolletildelingen deklarert i
`infra/keycloak/realm-havbruksbrukt.json` og opprettes automatisk når
Keycloak importerer realmet (`compose.yaml` kjører `start-dev
--import-realm`). Bare klienthemmeligheten er bevisst utelatt fra denne
filen, se under. I andre miljøer må drift fortsatt opprette klienten selv med
de samme rettighetene.

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

Klienten `marketplace-provisioner` opprettes automatisk ved import (se over),
men uten en fast hemmelighet i filen. Keycloak genererer derfor en ny
tilfeldig hemmelighet ved hver realm-import, f.eks. etter
`scripts/demo.sh wipe`. `scripts/demo.sh up` og `restart` henter gjeldende
hemmelighet fra Keycloak (med `kcadm.sh` inne i Keycloak-containeren) og
skriver den til den git-ignorerte `.env`-filen med eiertilgang (`600`) før
appen starter:

```bash
KEYCLOAK_ADMIN_ENABLED=true
KEYCLOAK_ADMIN_BASE_URL=http://localhost:8180
KEYCLOAK_ADMIN_REALM=havbruksbrukt
KEYCLOAK_ADMIN_CLIENT_ID=marketplace-provisioner
KEYCLOAK_ADMIN_CLIENT_SECRET=<synkronisert fra Keycloak>
```

Andre linjer i `.env` beholdes. Kjører appen fra IntelliJ (eller en annen
kjøring utenfor `scripts/demo.sh`), kopier de samme variablene fra `.env`
til kjørekonfigurasjonens miljøvariabler, og gjør det på nytt etter hver
`wipe`.

Etter restart kan `buyer-demo` sende en søknad og `admin-demo` godkjenne den.
Logg deretter ut og inn igjen som samme identitet for at den nye
`SELLER`-rollen skal være med i OIDC-sesjonen. Ikke legg `.env` eller
klienthemmeligheten i repository.

Hvis godkjenning likevel feiler med «Kunne ikke tildele selgerrollen akkurat
nå»: sjekk applikasjonsloggen (`scripts/demo.sh logs` eller kjøringens
konsoll) for linjen `Kunne ikke tildele SELLER-rollen for subject ... hos
Keycloak: ...` - den viser den underliggende feilen (f.eks. `client_not_found`
hvis klienten mangler i Keycloak, eller 403 hvis service-kontoen mangler en
av de tre rollene over).


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

- **«Connection lost, trying to reconnect...» (evig løkke) eller grå/tom
  skjerm på `/admin` eller `/app` etter innlogging**: Dette skyldes IKKE
  Vaadins utviklerverktøy eller devmode-visning. Vaadin er kartlagt på
  servlet-roten (`vaadin.url-mapping` er ikke satt, standard er `/*`), og
  rammeverkets interne init/uidl/heartbeat-forespørsler sendes derfor alltid
  til nøyaktig `/` - samme sti som den offentlige, Thymeleaf-rendrede
  forsiden (`PublicMarketplaceController`). To uavhengige krasj kan oppstå
  her samtidig:
  - Spring MVCs `RequestMappingHandlerMapping` fanger opp Vaadins interne
    GET-/POST-forespørsler til `/` FØR Vaadin selv får behandlet dem
    (enten stille, med feil innhold, eller med 405 Method Not Allowed).
  - Spring Securitys CSRF-beskyttelse blokkerer Vaadins interne
    POST-forespørsler (uidl/heartbeat) fordi de ikke bærer et gyldig
    CSRF-token.

  Løsningen (i `PublicMarketplaceController` og `SecurityConfiguration`)
  gjenkjenner Vaadins interne forespørsler på spørreparameteren `v-r`
  (`ApplicationConstants.REQUEST_TYPE_PARAMETER`): kontrolleren videresender
  dem til Vaadins egen `vaadinForwardingController`-bean i stedet for å
  rendre forsiden, og sikkerhetskonfigurasjonen unntar dem fra
  CSRF-sjekken. Unntaket bruker Vaadins `RequestUtil.isFrameworkInternalRequest`
  og gjelder bare ekte interne forespørsler til servlet-roten (samt
  `VAADIN/push` og `VAADIN/dynamic/...`). Et unntak for *enhver* forespørsel
  med `v-r` lot f.eks. `POST /logout?v-r=x` og `POST /selgersoknad?v-r=x`
  omgå CSRF-beskyttelsen. Tidligere forsøk på å fikse dette
  (`vaadin.eager-server-load=true`) flyttet bare symptomet fra grå skjerm
  til evig reconnect-løkke, uten å løse den underliggende
  sti-kollisjonen - denne innstillingen skal derfor IKKE settes.

- **IntelliJ viser feil som «`org.springframework.transaction.annotation`
  does not exist»**: Dette er ikke en reell kompileringsfeil (`./mvnw
  compile`/`verify` bekrefter dette gjentatte ganger), men et tegn på at
  IntelliJ aldri har importert prosjektet som et Maven-modul (ingen
  `.iml`-fil i `.idea/`). Løsning: åpne Maven-verktøyvinduet i IntelliJ og
  trykk «Reload All Maven Projects» (eller høyreklikk `pom.xml` → Add as
  Maven Project). Du trenger ikke kjøre appen fra IntelliJ for å teste
  demoen — `scripts/demo.sh` bygger og starter appen med riktig,
  verifisert classpath.

- **Utlogging er bare `POST /logout` med CSRF-token**: `GET /logout` logger
  ikke ut. Ellers kunne et annet nettsted logge brukeren ut av både
  markedsplassen og Keycloak-SSO-økten bare ved å lenke dit. «Logg ut» er
  derfor et lite skjema: `LogoutForm` i Vaadin-flatene (`/admin`, `/app`)
  og et Thymeleaf-skjema på `/selgersoknad`. Et skjema fanges heller ikke
  opp av Vaadins klientsideruter, som tidligere ga «could not navigate to
  logout» / «no route for logout» for en vanlig `<a href="/logout">`.
  Å skrive `http://localhost:8080/logout` i adressefeltet logger derfor
  ikke ut; bruk knappen.

- **Logger ut og inn igjen som en annen demobruker uten at
  Keycloak-innloggingsskjemaet vises**: Uten RP-initiated logout mot
  Keycloak avslutter `/logout` bare den lokale Spring-økten - Keycloaks
  egen SSO-økt (informasjonskapselen på `localhost:8180`) lever videre.
  Neste innlogging gjenbruker da stille den forrige identiteten i stedet
  for å vise skjemaet på nytt. `SecurityConfiguration` bruker nå
  `OidcClientInitiatedLogoutSuccessHandler` slik at `/logout` også sender
  brukeren via Keycloaks `end_session_endpoint`, som avslutter SSO-økten.

- **Godkjenning av selgersøknad feiler med «Kunne ikke tildele
  selgerrollen akkurat nå. Prøv igjen senere.»**: Sjekk først
  applikasjonsloggen - `KeycloakSellerRoleProvisioner` logger nå den
  underliggende Keycloak-feilen på ERROR-nivå (subject, statuskode/melding
  og full stack trace) i stedet for å svelge den stille. Vanligste
  årsaker lokalt: `marketplace-provisioner`-klienten finnes ikke ennå i
  Keycloak (`client_not_found` - importer realmet på nytt med
  `scripts/demo.sh wipe && scripts/demo.sh up`), eller
  `KEYCLOAK_ADMIN_CLIENT_SECRET` samsvarer ikke med hemmeligheten i
  Keycloak (401 `unauthorized_client` / «Invalid client credentials»).
  Det siste skjer når realmet er importert på nytt, fordi Keycloak da
  genererer en ny hemmelighet. `scripts/demo.sh restart` synkroniserer
  hemmeligheten til `.env` på nytt; kjører appen utenfor skriptet, oppdater
  miljøvariabelen fra `.env`.

## Produksjonsgrenser

Compose-filen kjører Keycloak med `start-dev` og en lokal, ukryptert
utviklingsdatabase. Produksjon må kjøre Keycloak separat med HTTPS, sikker
hemmelighetsforvaltning, særskilt realm/klient, administratorkonto og
produksjonsdatabase. Overstyr OIDC discovery gjennom den dokumenterte
`KEYCLOAK_ISSUER_URI`-miljøvariabelen; aldri legg produksjonsverdier i
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
