# Progresjon

Sist oppdatert: 2026-09-24

## Levert

- Grunnstruktur med Spring Boot, Java 24, Vaadin Flow og Thymeleaf.
- Offentlig server-rendered SEO-flate:
  - `/`
  - `/utstyr`
  - `/utstyr/{slug}`
- Server-side søk og filter på søkeord, lokasjon og tilstand.
- `canonical` og `robots` metadata på offentlige sider.
- `noindex,follow` for filtrerte/nulltreff-sider.
- `robots.txt`, `sitemap.xml` og nyttig server-rendered 404-side.
- Tilgjengelig filterskjema med semantiske labels og synlig keyboard focus.
- TDD-tester for servicefiltrering, metadata, crawler-endepunkter og 404.
- Dokumenterte arkitekturvalg for Vaadin/SEO og server-rendered SEO-prototype.
- Etablert domeneglossar i `CONTEXT.md` for annonse, selger, kjøper, utstyr,
  utstyrskategori, dokumentasjon, verifisert selger og forespørsel.
- Skilt domenemodellen tydeligere med `Seller` og `EquipmentCategory` i
  tillegg til `Listing`.
- Innført `DRAFT`, `PUBLISHED` og `ARCHIVED` som publiseringsstatus på en
  annonse; katalog-seamen eksponerer bare publiserte annonser.
- Låst Spring Boot-styrt Log4j-versjon til `2.25.5` som forebyggende
  dependency-overstyring.
- Valgt domeneobjekter uten JPA-annotasjoner og Data Mapper ved en kommende
  PostgreSQL/JPA-adapter.
- Implementert PostgreSQL/JPA-adapteren for katalog-seamen med Flyway som
  eneste skjemaeier og Testcontainers-integrasjonstester.
- Lagt til lokal PostgreSQL 17-tjeneste i Docker Compose; den samme
  Flyway-migreringsrekken brukes lokalt, i integrasjonstester og senere mot
  Aiven.
- Etablert lokal Keycloak-baseret OIDC-innlogging for `/app` med Authorization
  Code og PKCE. `SELLER`, `BUYER` og `ADMIN` er Keycloak realm-roller; `/app`
  krever `SELLER`.
- Introdusert `SellerAccount` som en idempotent, persistet kobling mellom
  Keycloak-subject (`sub`) og domenets `Seller`. Kontoen opprettes ved første
  innlogging som `SELLER` og eier kommende utkast og annonser.
- Implementert første utkastflyt i selgerområdet. Et utkast er `DRAFT`, får
  eier gjennom `SellerAccount`, og listes bare for den innloggede selgeren.
- Fullført første selgerflyt fra utkast til publisert annonse. Publisering
  kontrollerer eierskap på serveren og gjør deretter annonsen synlig i den
  eksisterende offentlige katalogen, detaljsiden og sitemapet.
- Besluttet sikker administrator- og selgergodkjenningsflyt: Keycloak-
  plattformadministrator og markedsplassens `ADMIN`-rolle er separate; nye
  identiteter får ingen publiseringsrettigheter uten godkjenning.
- Rettet arkitekturavvik: Vaadin er kartlagt på `/app/*` som i
  beslutningsnotat 0002/0003, med administrasjonen på `/app/admin`.
  `VaadinSecurityConfigurer` håndhever `@RolesAllowed` ved hver navigasjon
  (tidligere kunne en anonym bruker navigere klientside til administrasjonen),
  og workarounds fra rot-kartleggingen er fjernet.
- Min side (#16): alle innloggede personer registrerer virksomheter med
  organisasjonsnummer (kontrollsiffer), som gir en tilknytning som venter på
  verifisering (0007, userflow 11). Personen lagres bare som
  Keycloak-subject. Vanlig «Logg inn» lander på Min side, og menyen viser
  «Min side».
- Appmeny (#17): felles Vaadin-layout (`@Layout`, `AppLayout`) på alle
  appsider med Min side, Administrasjon for administrator, Selgerområde for
  `SELLER` inntil #19, Til nettstedet og Logg ut. Gjeldende side markeres med
  `aria-current`. Markedsplassadministratoren er en egen innlogging som ikke
  handler (0010) og har bare Administrasjon og Logg ut.
- Verifisering av tilknytninger (#18): administrasjonen viser tilknytninger
  som venter og en oversikt over alle, med hvem som avgjorde, når og hvorfor.
  Verifiser, avvis og trekk tilbake følger livssyklusen i domenet; avvisning
  og tilbaketrekking krever begrunnelse, som personen ser på Min side.
  Auditsporet (V8) er bare til å legge til. Optimistisk låsing hindrer
  doble beslutninger ved flere administratorer (0010). Navn og e-post slås
  opp i Keycloak ved vurdering og lagres ikke. Den gamle selgersøknaden er
  holdt tydelig adskilt til den fjernes (#26).

## Nåværende arkitektur

| Flate | Teknologi | Status |
| --- | --- | --- |
| Offentlig lesing og SEO | Spring MVC + Thymeleaf | Prototype fungerer |
| Innlogget appflate | Vaadin Flow + Keycloak OIDC under `/app/*` | Min side (`/app`) for alle innloggede, `/app/selgeromrade` krever `SELLER`, `/app/admin` krever `ADMIN`; `@RolesAllowed` håndheves av Vaadin |
| Data | PostgreSQL + Flyway + JPA Data Mapper | Aktiv katalogadapter; in-memory-adapter beholdes for enhetstester |
| Identitet og tilgang | Keycloak OIDC + Spring Security | Lokal demo-realm; produksjonskonfigurasjon gjenstår |
| Administrator og godkjenning | Beslutning 0006 + userflow 7 | Ikke implementert; må være på plass før selvregistrering |
| Selgereierskap | `SellerAccount` + Flyway V4 | Utkast eies og listes per selgerkonto |
| Designkilde | Eksisterende designmanual + mockups | Figma-designsystem skal formaliseres |

## Kvalitetssignal

- `./mvnw verify` passerer med PostgreSQL 17 i Testcontainers.
- Flyway migrerer `listing`-katalogen før Hibernate validerer skjemaet.
- JPA-entiteter og Spring Data-repository er avgrenset til persistence-adapteren;
  web- og domenelag bruker fortsatt bare domenemodellen og katalog-seamen.
- Offentlige SEO-responser verifiseres gjennom MockMvc, ikke bare visuell rendering.
- Ingen kommersielle Vaadin-avhengigheter eller eksterne SEO-tjenester er lagt til.
- Canonical- og sitemap-URL-er er beskyttet mot Host-header-forgiftning via konfigurert `APP_PUBLIC_BASE_URL`.
- Figma MCP-innlogging er verifisert; full skriveplass er tilgjengelig i studentteamet `Me`.
- `CONTEXT.md` er den kanoniske kilden for domenebegreper i kode, tester,
  tickets og beslutningsnotater.
- `log4j2.version` er satt til `2.25.5`; dependency-grafen inneholder
  foreløpig ikke `log4j-api` eller `log4j-to-slf4j`.

## Før neste funksjonelle milepæl

1. La offentlig katalog, annonsedetalj og sitemap bare bruke
   `PUBLISHED`-annonser; utkast og arkiverte annonser skal gi 404 offentlig.
2. Opprett Figma-artefakt for offentlig katalog, annonsedetalj og
   selgerinngang basert på eksisterende designmanual.
3. Implementer selgersøknad, administratorflate, auditlogg og trygg
   rolletildeling før selvregistrering aktiveres.
4. Implementer redigering og arkivering av egne annonser med server-side
   eierskapskontroll.
5. Koble kontaktforespørsel fra offentlig annonsedetalj til autentisert eller
   eksplisitt gjesteprosess.

## Neste naturlige steg

Neste funksjonelle steg er redigering og arkivering av **egne** annonser.
`SellerAccount` er den persisted eieren, og alle muterende operasjoner skal
slå opp annonsen med både slug og OIDC-subject på serveren.
`DRAFT`/`PUBLISHED`/`ARCHIVED` er en del av domenet: bare publiserte annonser
er synlige i katalog, annonsedetalj og sitemap.
