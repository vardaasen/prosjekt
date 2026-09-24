# Sikkerhetsbaseline før selgerflyt

Dette er en prototype-baseline, ikke en full sikkerhetssertifisering. Punktene skal være avklart før løsningen får innlogging, opplasting eller kontaktdata.

## Gjennomgått nå

- **Identitet:** Lokal utvikling bruker Keycloak som en separat OIDC-leverandør.
  Markedsplass-appen lagrer ikke passord og bruker Authorization Code med PKCE
  for innlogging under `/app`.
- **Tilgang:** `/app` krever Keycloak-rollen `SELLER`; alle offentlige SEO-ruter
  forblir anonyme. Keycloak realm-roller mappes eksplisitt til Spring
  authorities med `ROLE_`-prefiks.
- **Selgereierskap:** En `SellerAccount` kobler Keycloaks stabile `sub` til en
  selger uten å kopiere bruker- eller passorddata. Kontoregistrering er
  idempotent, slik at gjentatt innlogging ikke oppretter flere selgerkontoer.
- **Utkast:** Nye utkast har en databasefremmednøkkel til eierens
  `SellerAccount`. Søk etter utkast filtreres på den innloggede kontoens
  OIDC-subject, slik at en selger ikke kan se en annens utkast.
- **Publisering:** En annonse kan bare publiseres når serveren finner den som
  et `DRAFT` med både slug og den innloggede selgerkontoens OIDC-subject.
  Optimistisk låsing avviser konkurrerende endringer i stedet for å
  overskrive dem.
- **Server-rendering:** Offentlige sider rendres på serveren; ingen brukerinput settes inn som rå HTML.
- **Thymeleaf escaping:** Tekst fra listingdata går via `th:text`, som reduserer XSS-risiko.
- **Inputkontroll:** `tilstand` avvises med `400` når verdien ikke er en kjent enum.
- **Host-header:** canonical- og sitemap-URL-er bruker `app.public-base-url`/`APP_PUBLIC_BASE_URL`, ikke requestens `Host`-header.
- **Eksponering:** `/app` ligger ikke i sitemap og er eksplisitt disallowed i `robots.txt`. Dette er ikke en tilgangskontroll.
- **Avhengigheter:** Ingen kommersielle eller eksterne SEO-tjenester er lagt til.
- **Logging:** Spring Boot-styrt Log4j-versjon er eksplisitt låst til
  `2.25.5` gjennom `log4j2.version`. Prosjektet løser foreløpig ikke
  `log4j-api` eller `log4j-to-slf4j`, men overstyringen gjelder dersom de
  introduseres transitivt.
- **Testbarhet:** Metadata, sitemap, robots og 404 verifiseres med HTTP-tester.

## Må gjøres før produksjon eller kontoer

### 1. Konfigurasjon og URL-er

- Bruk en konfigurert offentlig base-URL for canonical- og sitemap-lenker; ikke stol på vilkårlig `Host`-header bak proxy.
- Konfigurer trusted forwarded headers eksplisitt dersom appen kjører bak ingress eller load balancer.
- Ikke legg hemmeligheter i `application.properties`, repository eller Figma-artefakter.

### 2. Identitet og tilgang

- Keycloak er valgt som OIDC-leverandør. Lokal Compose-konfigurasjon bruker
  demo-realm og demo-brukere bare for utvikling; produksjon må bruke en
  separat realm, registrert klient, hemmelighetsforvaltning og administratorkonto.
- Håndhev autorisasjon server-side på hver selgeroperasjon; skjult navigasjon er ikke tilgangskontroll.
- La hver annonse referere til sin `SellerAccount` før utkastflyten innføres,
  og slå opp eieren på serveren ved redigering, publisering og arkivering.
- Skill roller for kjøper, selger og administrasjon.
- Beskytt state-changing requests med CSRF og sikre session cookies (`Secure`, `HttpOnly`, passende `SameSite`).

### 3. Input, data og filer

- Sett lengdegrenser på søk, fritekst og alle tekstfelt.
- Valider og normaliser slugs, lokasjon og enumverdier før lagring.
- Ved bilde-/dokumentopplasting: allowlist MIME/type, størrelsesgrense, viruskontroll, tilfeldig lagringsnavn og ikke-serverbar lagring.
- Unngå å logge e-post, tokens, dokumentinnhold eller andre personopplysninger.

### 4. HTTP og drift

- Legg til relevante sikkerhetsheadere: CSP, `X-Content-Type-Options`, `Referrer-Policy`, frame policy og HSTS når HTTPS er på plass.
- Rate-limit søk, kontaktforespørsler, innlogging og opplastinger.
- Bruk strukturert auditlogg for innlogging, tilgangsendringer, publisering og sletting.
- Hold Spring/Vaadin/JDK-avhengigheter oppdatert og kjør dependency scanning i CI.
- Kontroller den løste dependency-grafen når nye Spring- eller
  logging-avhengigheter innføres; ikke anta at en versjonsoverstyring alene
  betyr at en artefakt finnes i runtime.

## Prioritet

| Prioritet | Tiltak | Når |
| --- | --- | --- |
| Høy | Konfigurert base-URL og trusted proxy-oppsett | Før sitemap/canonical går til staging |
| Høy | Autorisasjon for hver selgeroperasjon, CSRF og session-policy | Før `/app` får muterende funksjoner |
| Høy | Persistens med publiseringsstatus og server-side autorisasjon | Før selgerflyt |
| Medium | Inputgrenser, rate limiting og auditlogg | Før ekstern testbruk |
| Medium | Opplastingssikkerhet | Før bilder/dokumenter |
| Lavere | CSP-tuning og automatisert dependency scanning | Før produksjonssetting |

## Beslutning

Vi går ikke videre med selgeroppretting eller kontaktdata før punktene med høy prioritet er implementert eller eksplisitt akseptert som prototypebegrensninger. Figma kan brukes nå til å beskrive flyt og states, men skal ikke inneholde ekte brukerdata, tokens eller hemmeligheter.
