# Progresjon

Sist oppdatert: 2026-09-23

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

## Nåværende arkitektur

| Flate | Teknologi | Status |
| --- | --- | --- |
| Offentlig lesing og SEO | Spring MVC + Thymeleaf | Prototype fungerer |
| Innlogget appflate | Vaadin Flow under `/app` | Minimal startflate |
| Data | In-memory katalogadapter | PostgreSQL/JPA-adapter er neste skive |
| Identitet og tilgang | Ikke implementert | Må avklares før selgerfunksjoner |
| Designkilde | Eksisterende designmanual + mockups | Figma-designsystem skal formaliseres |

## Kvalitetssignal

- `./mvnw -q test` passerer.
- Offentlige SEO-responser verifiseres gjennom MockMvc, ikke bare visuell rendering.
- Ingen kommersielle Vaadin-avhengigheter eller eksterne SEO-tjenester er lagt til.
- Canonical- og sitemap-URL-er er beskyttet mot Host-header-forgiftning via konfigurert `APP_PUBLIC_BASE_URL`.
- Figma MCP-innlogging er verifisert; full skriveplass er tilgjengelig i studentteamet `Me`.
- `CONTEXT.md` er den kanoniske kilden for domenebegreper i kode, tester,
  tickets og beslutningsnotater.
- `log4j2.version` er satt til `2.25.5`; dependency-grafen inneholder
  foreløpig ikke `log4j-api` eller `log4j-to-slf4j`.

## Før neste funksjonelle milepæl

1. Implementer PostgreSQL/JPA-adapter for katalog-seamen med Flyway-migrering
   og Testcontainers-baserte integrasjonstester.
2. La offentlig katalog, annonsedetalj og sitemap bare bruke
   `PUBLISHED`-annonser; utkast og arkiverte annonser skal gi 404 offentlig.
3. Opprett Figma-artefakt for offentlig katalog, annonsedetalj og
   selgerinngang basert på eksisterende designmanual.
4. Implementer identitet, tilgangskontroll og sikker selgerflyt under `/app`
   først etter at sikkerhetsbaselinen er gjennomgått.
5. Koble kontaktforespørsel fra offentlig annonsedetalj til autentisert eller
   eksplisitt gjesteprosess.

## Neste naturlige steg

Neste tekniske steg er å innføre PostgreSQL/JPA-adapteren for den eksisterende
katalog-seamen, med Flyway og Testcontainers. `DRAFT`/`PUBLISHED`/`ARCHIVED`
er nå en del av domenet: bare publiserte annonser skal være synlige i katalog,
annonsedetalj og sitemap. Dette gjør SEO, 404, tilgangskontroll og senere
selgerflyt deterministisk. Før selve selgerflyten bygges, må
sikkerhetsbaselinen i `docs/security-baseline.md` være gjennomgått.
