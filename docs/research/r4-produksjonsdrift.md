# R4 – Produksjonsdrift for MVP (issue #48)

Dato: 2026-09-25 · Status: research, grunnlag for beslutningsnotat · Styrende prinsipp: beslutningsnotat 0009 (innebygd personvern og sikkerhet, GDPR art. 25)

Mål: kjøre Havbruksbrukt (Java/Spring Boot 4.1 + Vaadin Flow 25.2 + Keycloak 26.x + PostgreSQL 17/Flyway) i produksjon med inviterte brukere, med data i EU/EØS, minst mulig driftsbyrde og en eksplisitt vurdering av hver ekstern tjeneste.

Kun primærkilder er brukt (leverandørenes egne dokumentasjons-, pris-, DPA- og API-sider). Påstander som ikke kunne bekreftes er merket **[Ikke bekreftet]**. Priser er uten mva. og hentet 2026-09-25; de kan endres.

---

## Kortversjon

- **Aiven kan levere databasen, men ikke appen i dag.** Aiven Apps (container-kjøring) er i «limited availability» og beskrives som *ikke* ment for offentlige webapper ([changelog](https://aiven.io/changelog/41f6f5cf-7cac-4f7e-adcb-1eefc6198d03), [aiven.io/apps](https://aiven.io/apps)). App og Keycloak må derfor kjøres et annet sted.
- **Aivens billige planer passer dårlig for produksjonsdata:** Free/Developer lar deg ikke velge sky/region; Hobbyist ($12/mnd) har ingen PITR og uklar backup-dekning. Første plan med PITR er Startup (fra $75/mnd) ([pris](https://aiven.io/pricing?product=pg), [backups](https://aiven.io/docs/products/postgresql/concepts/pg-backups)).
- **Anbefaling (forslag):** Alt hos **én europeisk PaaS – Clever Cloud (fransk selskap, region Paris)**: Spring Boot-appen som Java-applikasjon, Keycloak som ferdig *Keycloak add-on* (får egen PostgreSQL), og appens PostgreSQL som add-on med daglig backup. Grovt anslag **ca. €58–75/mnd**. Aiven beholdes som alternativ B dersom produkteier ønsker Aiven for databasen (da ca. €70 + $75/mnd for et oppsett med PITR).
- **Må ordnes uansett plattform:** Vaadin produksjonsbygg, én app-instans (eller sticky sessions), `server.forward-headers-strategy`, Keycloak med `start --optimized`, `hostname`, `proxy-headers`, helsesjekker på port 9000, og lavere DB-pool for Keycloak (standard 100 forbindelser).
- **Funn utenfor scope, men viktig:** prosjektet bruker Java 24, som Clever Cloud merker som EOL ([Clever Cloud Java-dok](https://www.clever.cloud/developers/doc/deploy/applications/java/java-jar/)); lokal Keycloak er 26.4.0 mens leverandører nå ruller ut 26.7.x-sikkerhetsoppdateringer ([Clever Cloud Keycloak-dok, changelog](https://www.clever.cloud/developers/doc/deploy/services/keycloak/)). Bør vurderes før produksjon.

---

## 1. Hva Aiven tilbyr i dag

### 1.1 PostgreSQL-planer

| Plan | Pris | Ressurser | Sky/region | Backup | PITR | Maks forbindelser |
|---|---|---|---|---|---|---|
| Free | $0 | 1 CPU, 1 GB RAM, 1 GB lagring | Kan ikke velge sky/region | «Single backup» | Nei | 20 |
| Developer | $5/mnd | 1 CPU, 1 GB RAM, 8 GB | Kan ikke velge sky/region; «non-production» | «Single backup» | Nei | 15 |
| Hobbyist | fra $12/mnd ($0,02/t) | 1 CPU, 1 GB RAM, 8 GB | DigitalOcean, Google Cloud, OVH | Se merknad | Nei | 25 |
| Startup | fra $75/mnd ($0,10/t) | 2–32 CPU, 4–192 GB RAM, 80 GB+ | AWS, DO, Google, Azure, OVH, UpCloud | 2 dager | Ja | 100 (Startup-4) |
| Business | fra $180/mnd | 2 VM-er (HA) | som over | 14 dager | Ja | 100+ |
| Premium | fra $270/mnd | 3 VM-er | som over | 30 dager | Ja | 100+ |

Kilder: [Aiven pris – PostgreSQL](https://aiven.io/pricing?product=pg) og [plan-pris-fanen](https://aiven.io/pricing?product=pg&tab=plan-pricing), [backups og retensjon](https://aiven.io/docs/products/postgresql/concepts/pg-backups), [forbindelsesgrenser](https://aiven.io/docs/products/postgresql/reference/pg-connection-limits).

Merknader:

- **Motstrid om Hobbyist-backup:** prissiden oppgir «single backup only», mens backup-dokumentasjonen oppgir retensjon «None» for Hobbyist. **[Ikke bekreftet hvilken som gjelder]** – begge betyr i praksis at man ikke kan gjenopprette til et tidspunkt.
- **Free-tier kan slås av ved inaktivitet**: «Aiven may power off free services with no continuative activity» ([free plan](https://aiven.io/docs/platform/concepts/free-plan)). Når en tjeneste uten backup slås av: «If there are no backups, all service data is lost» ([power cycle](https://aiven.io/docs/platform/concepts/service-power-cycle)). Free er dermed uegnet for produksjonsdata.
- Free og Developer: «Cannot select a specific cloud or region» ([pris](https://aiven.io/pricing?product=pg&tab=plan-pricing)). Det gjør det umulig å garantere EU-lokasjon → strider mot 0009.
- Planpris er «all-inclusive» (VM, nettverk, backup), timebasert fakturering ([service pricing](https://aiven.io/docs/platform/concepts/service-pricing)).
- Backup-mekanisme: daglig full backup + WAL hvert 5. minutt; PITR bygger på dette ([backups](https://aiven.io/docs/products/postgresql/concepts/pg-backups)).
- PostgreSQL 17 er støttet til 2029-11-08, 18 til 2030-11-07 ([EOL-tabell](https://aiven.io/docs/platform/reference/eol-for-major-versions)).

### 1.2 EU-regioner

Aiven har bl.a. Norge (UpCloud `upcloud-no-svg` Oslo; Azure Norway East/West), Sverige (Google `europe-north2`, UpCloud Stockholm, Azure), Finland (Google `europe-north1`, UpCloud Helsinki), Danmark (UpCloud København) og Tyskland (AWS, Google, UpCloud, Azure, OVH Frankfurt) ([liste over skyer](https://aiven.io/docs/platform/reference/list_of_clouds)).

Viktig kombinasjon: **Hobbyist er bare tilgjengelig på DigitalOcean, Google Cloud og OVH**; UpCloud (finsk) og dermed Oslo-regionen krever Startup eller høyere ([pris](https://aiven.io/pricing?product=pg&tab=plan-pricing)). Om Hobbyist tilbys i akkurat `google-europe-north1` er **[Ikke bekreftet]** (vises først i konsollen).

### 1.3 TLS og forbindelser

- «All traffic to Aiven services is always protected by TLS» ([TLS-sertifikater](https://aiven.io/docs/platform/concepts/tls-ssl-certificates)). Standard `sslmode=require` krypterer, men verifiserer ikke serveren; for `verify-full` må prosjektets CA-sertifikat lastes ned og brukes. Aivens Java-eksempel bruker `?sslmode=require` ([connect-java](https://aiven.io/docs/products/postgresql/howto/connect-java)). **Anbefaling:** `sslmode=verify-full` med Aiven-CA i appen, og `db-tls-mode=verify-server` + truststore i Keycloak ([Keycloak db](https://www.keycloak.org/server/db)).
- Forbindelsesgrenser: Hobbyist 25, Startup-4 100; kan justeres (25–60 000) med restart ([connection limits](https://aiven.io/docs/products/postgresql/reference/pg-connection-limits)).
- PgBouncer-pooling finnes kun fra Startup; transaksjonsmodus er standard og bryter bl.a. navngitte prepared statements med mindre `pgbouncer.max_prepared_statements` settes ([pooling](https://aiven.io/docs/products/postgresql/concepts/pg-connection-pooling)). For vår skala (én app + én Keycloak) er pooling i klienten (HikariCP / Keycloaks egen pool) tilstrekkelig; PgBouncer er ikke nødvendig.
- **Kollisjon å merke seg:** Keycloaks `db-pool-max-size` er 100 som standard ([Keycloak db](https://www.keycloak.org/server/db)). På Hobbyist (25) må den settes ned (f.eks. 10), ellers kan Keycloak og appen sulte hverandre.

### 1.4 Én tjeneste med to databaser, eller to tjenester?

- Teknisk: én Aiven PostgreSQL-tjeneste kan ha flere databaser (konsoll, `avn service database create` eller SQL) ([create-database](https://aiven.io/docs/products/postgresql/howto/create-database)). Siden dokumenterer ikke tak på antall databaser eller rolle-oppsett per database **[Ikke bekreftet i Aiven-dok]**; separate roller med `GRANT` kun på egen database er standard PostgreSQL.
- Vurdering mot 0009:
  - *For én tjeneste:* halv kostnad; minste privilegium kan fortsatt oppnås med to roller (app-rolle ser ikke `keycloak`-databasen og omvendt).
  - *Mot én tjeneste:* delt forbindelsestak, delt ytelse og delt feildomene; backup/PITR gjelder hele tjenesten, så en gjenoppretting for appen vil i praksis også påvirke Keycloak **[Aiven-dok om granularitet ikke lest – Ikke bekreftet]**; én kompromittert superbruker (`avnadmin`) når begge.
  - **Forslag:** For MVP holder én tjeneste med to databaser og to separate roller (ikke `avnadmin` i appene). Skiller man senere ut Keycloak, er det en ren migrering. I anbefalt arkitektur (kap. 5) løses dette ved at Keycloak-add-on på Clever Cloud har sin egen database.

### 1.5 Applikasjons-/containerkjøring hos Aiven

- **Aiven Apps** ble annonsert 8. april 2026 i «limited availability»; tilgang må forespørres; ingen pris oppgitt ([changelog](https://aiven.io/changelog/41f6f5cf-7cac-4f7e-adcb-1eefc6198d03)).
- Produktet beskrives som en *stateless* runtime for containere nær dataene, og «is not intended as a general-purpose web host for public-facing, high-scale web apps» ([aiven.io/apps](https://aiven.io/apps)). Oversiktsdokumentasjonen nevner brukstilfeller som interne verktøy, admin-dashboards og strømbehandling ([Aiven Apps-dok](https://aiven.io/docs/products/aiven-apps)).
- Dokumentasjonen sier ingenting om egne domener, WebSocket, sticky sessions eller priser **[Ikke bekreftet]**.
- **Konklusjon:** Aiven Apps passer ikke for Havbruksbrukt nå: offentlig SEO-flate, stateful Vaadin-sesjoner og Keycloak med offentlig innloggingsflate er akkurat det produktet ikke er ment for, og LA-status er ingen god MVP-grunnmur.

### 1.6 DPA/GDPR hos Aiven

- DPA er en del av vilkårene; kunden er behandlingsansvarlig, Aiven databehandler ([DPA](https://aiven.io/dpa), [vilkår](https://aiven.io/terms)).
- «If the Customer has selected a Subprocessor to provide the hosting within the [EEA], Aiven shall store the Personal Data within the EEA.» Overføringer utenfor EØS dekkes av SCC (2021/914). Sletting senest 90 dager etter opphør. 14 dagers varsel ved bytte av hosting-underleverandør ([DPA](https://aiven.io/dpa)).
- Underleverandører: hosting hos valgt sky (bl.a. Google Cloud EMEA, AWS EMEA, Microsoft Ireland, UpCloud Oy, OVH SAS, DigitalOcean LLC – sistnevnte USA-basert); kontrollplan i GCP Belgia og AWS Stockholm; Aiven-selskaper i mange land (bl.a. USA, Israel, Australia) kan se *metadata* i support, mens «Customer Data never leaves the location chosen by Customer» ([subprocessors](https://aiven.io/subprocessors)).
- Merk: flere amerikanskeide skyer (Google, AWS, Microsoft, DigitalOcean) er aktuelle underleverandører. Velger man **UpCloud** (finsk) eller **OVH** (fransk) som sky, er hele kjeden europeisk eid – men UpCloud krever Startup-plan.

---

## 2. Hvor app og Keycloak kan kjøre i EU

Krav fra stacken: offentlig HTTPS med eget domene, hemmeligheter/miljøvariabler, Java 24/25 eller Docker, minst ~1–2 GB RAM per prosess, og for Vaadin: én instans *eller* sticky sessions; ingen scale-to-zero (Vaadin-sesjoner og UI-state lever i HTTP-sesjonen).

### 2.1 Sammenligning

| | **Clever Cloud** (FR) | **Scaleway Serverless Containers** (FR) | **Google Cloud Run** (US-eid) | **Hetzner Cloud VM** (DE) |
|---|---|---|---|---|
| Eierskap | Europeisk, Nantes | Europeisk, Paris | Amerikansk | Europeisk |
| EU-region | Paris (egne DC-er), Roubaix/Gravelines, Warszawa m.fl. ([FAQ](https://www.clever.cloud/developers/doc/find-help/faq/)) | Paris m.fl. ([pris](https://www.scaleway.com/en/pricing/serverless/)) | bl.a. `europe-north1` (FI), `europe-north2` (SE) | Tyskland, Finland ([Hetzner Cloud](https://www.hetzner.com/cloud/)) |
| Kjøremodell | PaaS: Java/Maven/JAR eller Docker; `CC_JAVA_VERSION` (21 default; 25; 24 merket EOL) ([Java](https://www.clever.cloud/developers/doc/deploy/applications/java/java-jar/)) | Serverless containere; linux/amd64-image ([limitations](https://www.scaleway.com/en/docs/serverless-containers/reference-content/containers-limitations/)) | Serverless containere | Egen VM, egen Docker Compose, egen proxy, egen patching |
| Keycloak | **Ferdig add-on** (Java-instans + egen PostgreSQL + FS Bucket), GA ([Keycloak add-on](https://www.clever.cloud/developers/doc/deploy/services/keycloak/)) | Egen container | Egen container | Egen container |
| HTTPS + eget domene | Ja, CNAME; «Trusted SSL» på `*.cleverapps.io` ([domener](https://www.clever.cloud/developers/doc/develop/common-configuration/domain-names/)). Automatisk sertifikat for eget domene **[Ikke bekreftet i dok]** | Egendefinert domene via CNAME ([custom domain](https://www.scaleway.com/en/docs/serverless-containers/how-to/add-a-custom-domain-to-a-container/)); automatisk sertifikat **[Ikke bekreftet]** | Ja **[ikke hentet i denne runden]** | Selv (f.eks. Caddy/Let's Encrypt) |
| Hemmeligheter | Miljøvariabler; egen «Secrets & Transit»-side finnes ([Java-dok, env](https://www.clever.cloud/developers/doc/deploy/applications/java/java-jar/)) | «Secret environment variables», maks 200 ([limitations](https://www.scaleway.com/en/docs/serverless-containers/reference-content/containers-limitations/)) | Secret Manager **[ikke hentet]** | Selv |
| Sticky sessions | Ja, per app: `--enable-sticky-sessions` ([CLI config](https://www.clever.cloud/developers/doc/manage/cli/applications/configuration/)) | **[Ikke bekreftet]** → kjør maks 1 instans | «Best effort» cookie-affinitet ([session affinity](https://docs.cloud.google.com/run/docs/configuring/session-affinity)) | Én VM → ikke relevant |
| WebSocket | Leverandørblogg sier ja ([blogg 2013](https://www.clever.cloud/blog/features/2013/03/11/what-about-websockets-on-the-cloud/)); nyere dok **[Ikke bekreftet]** (ikke nødvendig i dag) | Forespørsler 10 s–60 min timeout ([limitations](https://www.scaleway.com/en/docs/serverless-containers/reference-content/containers-limitations/)) | Ja **[ikke hentet]** | Selv |
| Scale-to-zero-risiko | Nei (alltid-på instanser) | Ja: «Time before scale to zero: 15 minutes» – må settes min-scale ≥ 1 ([limitations](https://www.scaleway.com/en/docs/serverless-containers/reference-content/containers-limitations/)) | Ja, må sette min-instanser | Nei |
| Minimumspris (app) | Java XS (1,15 GB) €0,0222/t ≈ **€16/mnd**; S (2 GB) €0,0444/t ≈ **€32/mnd** ([Clever Cloud pris-API](https://api.clever-cloud.com/v4/billing/price-system?zone_id=par&currency=EUR), [instans-API](https://api.clever-cloud.com/v2/products/instances)) | €0,00001/vCPU-s + €0,000002/GB-s etter gratiskvote ([pris](https://www.scaleway.com/en/pricing/serverless/)); 1 vCPU + 2 GB døgnet rundt ≈ **€33/mnd** (egen utregning) | **[Ikke bekreftet]** – prissiden kunne ikke leses maskinelt | **[Ikke bekreftet]** – prisene lastes med JavaScript |
| DPA | [Clever Cloud DPA](https://www.clever.cloud/general-terms-and-conditions-of-use/clever-cloud-data-processing-agreement/): 30 dagers varsel om nye underleverandører, overføring utenfor EU etter GDPR kap. V; datasentre i «Paris Clever Cloud»-regionen er franske ([underleverandørliste v1.6, 2026-09-01](https://cdn.clever-cloud.com/uploads/2026/09/clever-clouds-sub-processors.pdf)) | [Scaleway DPA 2024](https://www-uploads.scaleway.com/DPA_2024_ENG_b0abb5cc26.pdf), fransk lov ([kontrakter](https://www.scaleway.com/en/contracts/)) | Google Cloud DPA **[ikke hentet]** | Hetzner DPA **[ikke hentet]** |

Utregning Scaleway (1 vCPU, 2 GB, 730 t ≈ 2 628 000 s): vCPU (2 628 000 − 200 000) × €0,00001 ≈ €24,3; minne (5 256 000 − 400 000) × €0,000002 ≈ €9,7; sum ≈ €34/mnd for én container. Gratiskvoten gjelder per konto, så container nr. 2 (Keycloak) koster fullt: ≈ €35/mnd.

Clever Cloud-merknader:
- Personopplysninger i Clever Clouds egne støttesystemer: Twilio (telefonsupport) og Pipedrive (CRM) ligger i USA ([underleverandører](https://cdn.clever-cloud.com/uploads/2026/09/clever-clouds-sub-processors.pdf)). Disse gjelder kontakt med *oss som kunde*, ikke sluttbrukerdata i appen – men bør nevnes i beslutningsnotatet.
- Nano/pico-instanser har redusert CPU-prioritet ([scaling](https://www.clever.cloud/developers/doc/develop/common-configuration/scaling/)); bruk minst XS/S for Spring Boot + Vaadin.

### 2.2 Vurdering

- **Clever Cloud** treffer best: europeisk eid, fransk datasenterkjede, ferdig Keycloak-tjeneste med oppgraderinger (lav driftsbyrde = «foretrekk managed»), sticky sessions som bryter, og PostgreSQL med daglig backup i samme leverandør.
- **Scaleway** er et godt europeisk alternativ, men krever at vi selv drifter Keycloak-image og tvinger min-scale = max-scale = 1 fordi sticky sessions ikke er bekreftet.
- **Cloud Run** fungerer teknisk (EU-regioner, cookie-affinitet), men er amerikanskeid og affiniteten er uttrykkelig «best effort» → svakere mot 0009.
- **Hetzner VM** er billigst i rå ressurser, men vi overtar OS-patching, TLS, brannmur og backup → strider mot «foretrekk managed» og «enkleste løsning» for et skoleprosjekt.

---

## 3. Keycloak i produksjon (keycloak.org)

| Tema | Krav/anbefaling | Kilde |
|---|---|---|
| Produksjonsmodus | Bygg eget image i to trinn: `kc.sh build` med `KC_DB=postgres`, `KC_HEALTH_ENABLED=true` (og `KC_METRICS_ENABLED=true`), deretter `start --optimized`. Ikke `start-dev` (som compose bruker lokalt i dag). | [Containers](https://www.keycloak.org/server/containers) |
| Hostname | Sett `hostname` til full URL, f.eks. `https://login.<domene>`; `hostname-strict` er `true` i produksjon, så dynamisk oppløsning fra headere skjer ikke. Egen `hostname-admin` kan skille admin-konsollen ut. | [Hostname](https://www.keycloak.org/server/hostname) |
| TLS ved proxy | Ved «edge termination» (PaaS-proxy tar TLS): `--http-enabled true` + `proxy-headers` (`xforwarded` eller `forwarded`). Keycloak omtaler edge som mindre sikker enn re-encrypt; på PaaS er edge normalt eneste valg. | [Hostname](https://www.keycloak.org/server/hostname), [Reverse proxy](https://www.keycloak.org/server/reverseproxy) |
| Proxy-tillit | Sett `proxy-trusted-addresses` til proxyens adresser/CIDR. Ikke bruk proxy-headere ved passthrough. | [Reverse proxy](https://www.keycloak.org/server/reverseproxy) |
| Hva eksponeres | Offentlig: `/realms/`, `/resources/`, `/.well-known/`. Ikke offentlig: `/admin/`, `/realms/master/`, management-port 9000 (`/health`, `/metrics`). Ikke eksponer rot `/`. | [Reverse proxy](https://www.keycloak.org/server/reverseproxy) |
| Database | `db=postgres`, `db-url`, `db-username`, `db-password`; PostgreSQL 14–18 støttet; `db-pool-max-size` standard 100; TLS med `db-tls-mode=verify-server` + truststore. | [Database](https://www.keycloak.org/server/db) |
| Helsesjekker | `/health/started`, `/health/live`, `/health/ready` på management-port 9000; aktiveres med `health-enabled` ved build; DB-sjekk krever `metrics-enabled`. | [Health](https://www.keycloak.org/observability/health) |
| Minne | «The base memory usage for a Pod including caches of Realm data and 10,000 cached sessions is 1250 MB of RAM». I container brukes 70 % av minnegrensen til heap + ca. 300 MB utenfor heap → grense = (mål − 300 MB)/0,7 ≈ 1,36 GB for 1250 MB. **Planlegg 1,5–2 GB.** | [Minne/CPU-dimensjonering](https://www.keycloak.org/high-availability/single-cluster/concepts-memory-and-cpu-sizing) |
| CPU | 1 vCPU per 15 passordinnlogginger/s, med 150 % margin – langt over MVP-behov. | samme |
| Last | Sett `http-max-queued-requests` for lastavvisning (503). | [Production config](https://www.keycloak.org/server/configuration-production) |
| Klynge | Ikke nødvendig for MVP; ved flere noder brukes sticky sessions på `AUTH_SESSION_ID`. | [Reverse proxy](https://www.keycloak.org/server/reverseproxy) |

Med Clever Cloud Keycloak add-on håndteres image, database og proxy av leverandøren: standardstørrelse er Java S + PostgreSQL XXS, egendefinert domene settes med `access-domain`, realms kan importeres fra FS Bucket, admin-bruker må bytte passord ved første innlogging, og det finnes IP-filtrering per endepunkt ([Keycloak add-on](https://www.clever.cloud/developers/doc/deploy/services/keycloak/)). Vi må likevel verifisere at add-on setter `hostname`/proxy riktig og at `/admin` kan begrenses **[Ikke bekreftet i detalj]**.

Øvrig: `infra/keycloak/realm-havbruksbrukt.json` er kun for lokal utvikling og skal ikke importeres i staging eller produksjon. Den inneholder demobrukere med kjente passord, blant annet `admin-demo` med `ADMIN`-rollen, som gir tilgang til administrasjonen. Opprett en separat produksjons-realm og klient uten demobrukere, og bootstrap navngitte administratorer utenfor appens HTTP-grensesnitt og repository (0006). Realm-import må ikke inneholde produksjonshemmeligheter i Git; klienthemmeligheten til appen settes som miljøvariabel. Om `--import-realm` oppfører seg likt under `start` som `start-dev` er **[Ikke bekreftet – import-dok ikke lest]**.

---

## 4. Vaadin produksjonsbygg og utrulling (vaadin.com)

- **Produksjonsbygg:** `mvn clean package` gir JAR med ferdig frontend-bunt; `-Dvaadin.force.production.build=true` tvinger optimalisert bunt; `-Dvaadin.ci.build=true` gir `npm ci`. `vaadin-dev` skal være `optional` så dev-serveren ikke havner i produksjonspakken ([Production build](https://vaadin.com/docs/latest/flow/production/production-build)). `pom.xml` har allerede `vaadin-dev` som `<optional>true</optional>`.
- **Flere instanser krever sticky sessions:** «Horizontal scaling requires configuring sticky sessions in the load balancer due to the stateful nature of Vaadin UIs» ([Distributed deployment](https://vaadin.com/docs/latest/flow/production/distributed-deployment)).
- **Sesjonsreplikering** krever at alt UI-state er `Serializable`, og Vaadins ferdige løsning (Kubernetes Kit) er kommersiell: «A commercial Vaadin subscription is required» ([Kubernetes Kit](https://vaadin.com/clustering)). Dette er utelukket uten beslutningsnotat (jf. 0001 open-first).
- **Reverse proxy:** proxy må sende videre forwarded-headere; for push kreves `Upgrade`-header og lange timeouts ([Reverse proxy](https://vaadin.com/docs/latest/flow/production/reverse-proxy)). Push brukes ikke i dag.
- **Konsekvens for MVP:** Kjør **én** app-instans. Ved redeploy/omstart mister innloggede brukere UI-state og må laste siden på nytt (og evt. logge inn igjen). Det er akseptabelt for en invitert MVP, men bør stå i beslutningsnotatet. Skal man gå til to instanser, slå på sticky sessions (Clever Cloud støtter det).

Spring Boot bak proxy (spring.io):
- Sett `server.forward-headers-strategy=NATIVE` (eller `FRAMEWORK`); den er kun auto-aktivert på plattformer Spring Boot gjenkjenner, ellers `NONE`. Ikke la `server.tomcat.remoteip.internal-proxies` være tom i produksjon ([Behind a proxy](https://docs.spring.io/spring-boot/how-to/webserver.html)). Dette er nødvendig for at OAuth2-redirect-URI-er blir `https://`.
- Helse: `/actuator/health/liveness` og `/actuator/health/readiness` ([Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)). Om actuator er med i prosjektet i dag er ikke sjekket.
- HikariCP-pool bør settes eksplisitt (f.eks. 5–10) slik at app + Keycloak holder seg under databasens forbindelsestak.

---

## 5. Anbefalt minimal arkitektur

### 5.1 Forslag A (anbefalt): alt hos Clever Cloud, region Paris

```
Bruker ──HTTPS──▶ Clever Cloud-proxy (Sōzu, TLS) ─▶ Havbruksbrukt (Java S, 1 instans)
                                               │        │  JDBC/TLS
                                               │        ▼
                                               │   PostgreSQL add-on (app, daglig backup, 7 dager)
                                               │
                   login.<domene> ─────────────┴▶ Keycloak add-on (Java + egen PostgreSQL + FS Bucket)
```

| Komponent | Plan | Pris/mnd (ekskl. mva.) | Kilde |
|---|---|---|---|
| App (Spring Boot + Vaadin) | Java S, 2 GB, 1 instans (XS 1,15 GB mulig, men trangt) | ≈ €32 (XS ≈ €16) | [pris-API](https://api.clever-cloud.com/v4/billing/price-system?zone_id=par&currency=EUR) |
| Keycloak | Keycloak add-on «base» | €37 | [add-on-API](https://api.clever-cloud.com/v2/products/addonproviders) |
| App-database | PostgreSQL `xxs_sml`: 512 MB RAM, 1 GB, 45 forbindelser, daglig backup, 7 dager | €5,25 (alt. `xs_tny` 1 GB RAM/2 GB disk: €15) | samme |
| **Sum** | | **≈ €58–74** (inntil ≈ €84 med `xs_tny`) | |

Usikkerhet: Hvorvidt €37 for Keycloak dekker både Java-instansen og Keycloaks PostgreSQL, eller om de faktureres i tillegg, er **[Ikke bekreftet]** – dokumentasjonen sier bare at add-on oppretter disse ressursene ([Keycloak add-on](https://www.clever.cloud/developers/doc/deploy/services/keycloak/)). Verifiser i Clever Clouds priskalkulator før beslutning. PITR på Clever-PostgreSQL krever at man kontakter support (pgBackRest) ([PostgreSQL](https://www.clever.cloud/developers/doc/deploy/databases/postgresql/)); DEV-planen er gratis men uten backup og kun for test (samme kilde).

### 5.2 Forslag B: Aiven for databasene + Clever Cloud for app og Keycloak

- Aiven PostgreSQL **Startup-4** på UpCloud (Helsinki/Oslo) eller OVH (Frankfurt) – europeisk eid sky, PITR 2 dager: ≈ $73–75/mnd ([pris](https://aiven.io/pricing?product=pg)). Én tjeneste, to databaser (`havbruksbrukt`, `keycloak`), to roller.
- App på Clever Cloud (€32) + Keycloak som egen Docker-app på Clever Cloud (Java S-ekvivalent, ≈ €32) mot Aiven-databasen, *eller* Keycloak add-on (€37, egen DB hos Clever).
- **Sum ≈ €64–69 + ≈ $75/mnd.** Gir PITR og Aivens drift, men to leverandører, to DPA-er, trafikk mellom leverandører over internett (TLS) og høyere latens app↔DB **[latens ikke målt]**.
- Billigvariant med Hobbyist ($12, Google/OVH/DO, ingen PITR, uklar backup): **frarådes** for produksjonsdata med ekte brukere.

### 5.3 Hvorfor A

- Enklest (én leverandør, én DPA, én konsoll), europeisk eid kjede og data i Frankrike, managed Keycloak-oppgraderinger, lavest pris.
- Aiven gir ingen verdi for app/Keycloak i dag (Apps er LA og ikke for offentlige webapper), og for databasen koster reell backup/PITR minst ~$75.

---

## 6. Utkast til beslutningsnotat (0011 – Produksjonsdrift for MVP)

**Problem**
Havbruksbrukt skal kjøres i produksjon for inviterte brukere. Løsningen består av en Spring Boot/Vaadin-app (stateful sesjoner), Keycloak og PostgreSQL. Vi trenger drift i EU/EØS som oppfyller 0009 (innebygd personvern, dataminimering, minste privilegium, managed mekanismer, enkleste løsning) til lav kostnad og lav driftsbyrde.

**Alternativer**
1. *Clever Cloud (Paris) for alt:* Java-app, Keycloak add-on, PostgreSQL add-on. ≈ €58–84/mnd. Europeisk eid; sticky sessions tilgjengelig; daglig backup 7 dager; PITR via support.
2. *Aiven PostgreSQL (Startup-4, UpCloud/OVH) + Clever Cloud for app/Keycloak:* ≈ €64–69 + $75/mnd. PITR, men to leverandører.
3. *Aiven Hobbyist + container-PaaS:* ≈ $12 + app. Forkastes: ingen PITR/uklar backup; Free/Developer har ikke valgbar region.
4. *Scaleway Serverless Containers + managed DB:* ≈ €34 per container + DB. Europeisk, men Keycloak må driftes selv og sticky sessions er ikke bekreftet.
5. *Google Cloud Run:* EU-region, men amerikanskeid og «best effort»-affinitet.
6. *Egen VM (Hetzner):* billig, men vi overtar patching, TLS og backup.
7. *Aiven Apps:* forkastes – limited availability, ikke ment for offentlige webapper.

**Valg** (forslag til produkteier)
Alternativ 1. Én app-instans, Java S. Keycloak add-on med eget domene `login.<domene>`. App-database på dedikert plan med daglig backup. Region Paris. Java-versjon løftes til 25 (LTS) før produksjon (egen sak).

**Konsekvens**
- Personopplysninger (navn, e-post, virksomhetstilknytning, annonser, innloggingsdata i Keycloak) lagres og behandles i Clever Clouds franske datasentre; Clever Cloud er databehandler etter sin DPA. Clever Clouds egne support/CRM-underleverandører i USA berører kun kundeforholdet.
- Omstart/redeploy nullstiller Vaadin-sesjoner; akseptert for MVP.
- Ingen PITR som standard (kun daglig backup, 7 dager); tap av inntil ~24 t data er mulig. Kan kjøpes ut via support eller ved å bytte til alternativ 2.
- Keycloak-admin skal ikke være åpent eksponert (IP-filtrering eller separat `hostname-admin`).
- Oppfølging: dokumentere secrets-håndtering, backup-/gjenopprettingstest, oppdatering av Keycloak til gjeldende 26.x, og konfigurasjon (`forward-headers-strategy`, DB-pooler, TLS-verifisering mot DB).

---

## 7. Åpne spørsmål til produkteier

1. Er **Aiven** et krav (f.eks. av læringshensyn eller avtale), eller et ønske? Hvis krav: aksepteres Startup-4 (~$75/mnd) for å få PITR, eller Hobbyist med risiko for datatap?
2. Hvilket **budsjett** per måned og hvem betaler (skole, student, bedrift)? Hvem står som behandlingsansvarlig og signerer DPA?
3. Kreves data i **Norge** spesifikt, eller holder EU/EØS (Frankrike)? Norge peker mot Aiven på UpCloud Oslo/Azure Norway (kun Startup+) og en annen app-plattform.
4. Hvilket **domene** skal brukes (app og `login.`-subdomene), og hvem eier DNS?
5. Akseptabelt **RPO/RTO**: tåles tap av inntil ett døgn med data? Hvor lenge kan tjenesten være nede?
6. **E-post fra Keycloak** (invitasjoner, glemt passord) krever SMTP-leverandør – egen ekstern tjeneste som trenger egen vurdering etter 0009.
7. Skal Java løftes fra 24 (EOL) til 25 LTS før produksjon?
8. Trenger vi **overvåking/logger** utover det plattformen gir (f.eks. feilvarsling)? Hver slik tjeneste er en ny databehandler.

---

## 8. Ikke bekreftet / hull i kildene

- Om Hobbyist har én backup eller ingen (Aivens pris- og dokumentasjonssider sier ulikt).
- Om Hobbyist kan kjøres i en bestemt EU-region på Google (f.eks. `europe-north1`).
- Om Aiven PITR/gjenoppretting er per tjeneste (antatt) – ikke lest.
- Aiven Apps: pris, domener, WebSocket, sticky sessions – ikke dokumentert.
- Clever Cloud: hva €37 for Keycloak add-on inkluderer; automatisk TLS-sertifikat for eget domene; WebSocket i nyere dokumentasjon; hvordan add-on setter `hostname`/`proxy-headers` og begrenser `/admin`.
- Scaleway: sticky sessions og automatisk sertifikat for eget domene.
- Google Cloud Run og Hetzner: priser og DPA ble ikke hentet (sidene kunne ikke leses maskinelt); de er derfor bare sammenlignet kvalitativt.
- Keycloak `--import-realm` under `start` (produksjonsmodus) – importdokumentasjonen ble ikke lest.
- Clever Cloud-priser er hentet fra leverandørens offentlige pris-API (timepris × 730 t), ikke fra en lesbar prisside; kontroller i kalkulatoren.

## Kilder

Aiven: [pris PG](https://aiven.io/pricing?product=pg) · [plan-priser](https://aiven.io/pricing?product=pg&tab=plan-pricing) · [free plan](https://aiven.io/docs/platform/concepts/free-plan) · [backups](https://aiven.io/docs/products/postgresql/concepts/pg-backups) · [connection limits](https://aiven.io/docs/products/postgresql/reference/pg-connection-limits) · [pooling](https://aiven.io/docs/products/postgresql/concepts/pg-connection-pooling) · [create database](https://aiven.io/docs/products/postgresql/howto/create-database) · [skyer/regioner](https://aiven.io/docs/platform/reference/list_of_clouds) · [connect Java](https://aiven.io/docs/products/postgresql/howto/connect-java) · [TLS](https://aiven.io/docs/platform/concepts/tls-ssl-certificates) · [service pricing](https://aiven.io/docs/platform/concepts/service-pricing) · [power cycle](https://aiven.io/docs/platform/concepts/service-power-cycle) · [EOL-versjoner](https://aiven.io/docs/platform/reference/eol-for-major-versions) · [DPA](https://aiven.io/dpa) · [subprocessors](https://aiven.io/subprocessors) · [Aiven Apps](https://aiven.io/apps) · [Aiven Apps-dok](https://aiven.io/docs/products/aiven-apps) · [Aiven Apps LA-changelog](https://aiven.io/changelog/41f6f5cf-7cac-4f7e-adcb-1eefc6198d03)

Clever Cloud: [Keycloak add-on](https://www.clever.cloud/developers/doc/deploy/services/keycloak/) · [Java JAR](https://www.clever.cloud/developers/doc/deploy/applications/java/java-jar/) · [scaling](https://www.clever.cloud/developers/doc/develop/common-configuration/scaling/) · [PostgreSQL](https://www.clever.cloud/developers/doc/deploy/databases/postgresql/) · [CLI config / sticky sessions](https://www.clever.cloud/developers/doc/manage/cli/applications/configuration/) · [domener](https://www.clever.cloud/developers/doc/develop/common-configuration/domain-names/) · [FAQ](https://www.clever.cloud/developers/doc/find-help/faq/) · [DPA](https://www.clever.cloud/general-terms-and-conditions-of-use/clever-cloud-data-processing-agreement/) · [underleverandører v1.6](https://cdn.clever-cloud.com/uploads/2026/09/clever-clouds-sub-processors.pdf) · [pris-API](https://api.clever-cloud.com/v4/billing/price-system?zone_id=par&currency=EUR) · [instans-API](https://api.clever-cloud.com/v2/products/instances) · [add-on-API](https://api.clever-cloud.com/v2/products/addonproviders)

Scaleway: [begrensninger](https://www.scaleway.com/en/docs/serverless-containers/reference-content/containers-limitations/) · [pris](https://www.scaleway.com/en/pricing/serverless/) · [eget domene](https://www.scaleway.com/en/docs/serverless-containers/how-to/add-a-custom-domain-to-a-container/) · [DPA 2024](https://www-uploads.scaleway.com/DPA_2024_ENG_b0abb5cc26.pdf)

Google: [Cloud Run session affinity](https://docs.cloud.google.com/run/docs/configuring/session-affinity) · Hetzner: [Cloud](https://www.hetzner.com/cloud/)

Keycloak: [production](https://www.keycloak.org/server/configuration-production) · [reverse proxy](https://www.keycloak.org/server/reverseproxy) · [containers](https://www.keycloak.org/server/containers) · [hostname](https://www.keycloak.org/server/hostname) · [db](https://www.keycloak.org/server/db) · [health](https://www.keycloak.org/observability/health) · [sizing](https://www.keycloak.org/high-availability/single-cluster/concepts-memory-and-cpu-sizing)

Vaadin: [production build](https://vaadin.com/docs/latest/flow/production/production-build) · [distributed deployment](https://vaadin.com/docs/latest/flow/production/distributed-deployment) · [reverse proxy](https://vaadin.com/docs/latest/flow/production/reverse-proxy) · [Kubernetes Kit](https://vaadin.com/clustering)

Spring: [behind a proxy](https://docs.spring.io/spring-boot/how-to/webserver.html) · [actuator probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
