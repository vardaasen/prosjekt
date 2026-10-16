# Havbruksbrukt

**Havbruksbrukt** er en B2B-markedsplass for kjøp og salg av brukt akvakulturutstyr mellom virksomheter. Løsningen kombinerer åpne, søkemotorvennlige katalogsider med en innlogget arbeidsflate der personer knyttes til virksomheter, lager annonser og får dem kontrollert av en uavhengig markedsplassadministrator før publisering. Selve betalingen skjer utenfor markedsplassen.

Prosjektet er under utvikling og står i dag som en fungerende prototype.

---

## Problem og behov

Brukt utstyr i havbruksnæringen – som pumper, merder, fôringsanlegg og arbeidsbåter – har høy verdi, men omsettes ofte gjennom uformelle kanaler. I handel mellom virksomheter er **tillit** hovedutfordringen:

- Kjøper må vite hvilken virksomhet som faktisk selger utstyret, hvilken tilstand det er i, og om personen som har lagt ut annonsen har fullmakt til å opptre på vegne av virksomheten.
- I B2B er ikke «kjøper» og «selger» faste kontotyper. Den samme virksomheten kan selge overskuddsutstyr i én handel og kjøpe i den neste.
- Personer bytter jobb eller roller. Derfor må annonser, bud og handler tilhøre **virksomheten** (identifisert med organisasjonsnummer), ikke enkeltpersonens brukerkonto.

## Målgruppe

Markedsplassen er laget for **virksomheter** i havbruksnæringen, representert ved innloggede **personer**:

- **Virksomheter som kjøper:** Innkjøpere, driftsledere og teknisk personell som søker, filtrerer og vurderer brukt utstyr på tvers av kategori, lokasjon og tilstand.
- **Virksomheter som selger:** Oppdrettsselskaper og leverandører som vil registrere virksomheten sin, utarbeide annonseutkast sammen med kollegaer og få utstyret publisert i en ryddig og etterprøvbar kanal.
- **Markedsplassadministrator:** En uavhengig kontrollrolle med egen innlogging (som aldri selv deltar i handel). Administratoren verifiserer tilknytninger mellom personer og virksomheter, godkjenner annonser før de blir offentlige, og verifiserer handler.

## Viktigste funksjoner (i prototypen nå)

- **Offentlig katalog og søkemotoroptimalisering (SEO):**
  - Server-rendret forside (`/`), katalog (`/utstyr`) og annonsedetalj (`/utstyr/{slug}`) med søk og filtrering på søkeord, lokasjon og tilstand.
  - Automatisk `sitemap.xml` for publiserte annonser, `robots.txt`, kanoniske URL-er og utskriftsvennlig A4-visning for bruk som beslutningsgrunnlag.
- **Sikker innlogging og rollebasert navigasjon:**
  - Innlogging via Keycloak (OpenID Connect med PKCE) og sikker utlogging som avslutter økten både i appen og hos identitetsleverandøren.
  - Vanlige innloggede personer sendes til *Min side* (`/app`), mens markedsplassadministrator rutes direkte til *Administrasjon* (`/app/admin`).
- **Min side – registrering av virksomhet og tilknytning:**
  - Innloggede personer kan registrere en virksomhet med validering av norsk organisasjonsnummer (ni sifre og kontrollsiffer) og opprette en tilknytning som venter på kontroll.
- **Verifisering av tilknytninger:**
  - Markedsplassadministratoren kontrollerer ventende tilknytninger og kan verifisere, avvise (med begrunnelse) eller trekke tilbake en tilknytning. Navn og e-post slås opp fra Keycloak ved vurdering og lagres ikke unødig i appens database (dataminimering).
- **Annonseflyt fra utkast til publisert – eid av virksomheten:**
  - Personer med verifisert tilknytning kan opprette og redigere annonseutkast på vegne av virksomheten, og sende dem til godkjenning.
  - Administratoren vurderer annonsen og kan enten godkjenne den (slik at den blir offentlig synlig i katalogen) eller be om endringer med tilbakemelding.
- **Sporbarhet og universell utforming:**
  - Alle beslutninger om tilknytninger og annonser logges i et uforanderlig auditspor, og optimistisk låsing hindrer at to brukere overskriver hverandre.
  - Skjemaer, feilmeldinger og feilsider følger prinsippene for universell utforming (WCAG 2.2).

## Teknologier

| Område | Teknologi |
| --- | --- |
| **Språk og bygg** | Java 24, Maven (`./mvnw`) |
| **Backend** | Spring Boot 4.1 (Spring MVC, Spring Security, OAuth2 Client, Validation, Data JPA) |
| **Offentlig flate (SEO)** | Thymeleaf (server-rendered HTML) og CSS (inkludert eget utskriftsstilark) |
| **Innlogget appflate (`/app/*`)** | Vaadin Flow 25.2 (åpne Vaadin Core-komponenter og Aura-tema) |
| **Database og migrering** | PostgreSQL 17, Flyway (skjemaversjonering), ren domenemodell med Data Mapper |
| **Identitet og tilgang** | Keycloak 26.x (OIDC / OAuth2), kjørt i Docker Compose lokalt |
| **Testing** | JUnit 5, Spring MockMvc, Spring Security Test, Testcontainers (PostgreSQL) |
| **Sikkerhet og AI-verktøy** | Aikido Security, GitHub Copilot, Claude Code, egendefinerte Agent Skills og MCP (Vaadin/Figma) |

## Arbeidsfordeling

- **Willads Storm-Ritter (`vardaasen`) – Operatør, produkteier og arkitekt:**
  - Prosjektet er gjennomført som et individuelt arbeid der utvikleren har styrt arkitektur, domenemodellering, kravspesifikasjon og kvalitetssikring, med AI-agenter (GitHub Copilot og Claude Code) og Aikido Security som verktøy i utviklingsløpet.
  - Arbeidet er styrt gjennom en fast kjede dokumentert i repoet: **Design og brukerflyter → Domenemodell og ordliste → Beslutningsnotater → Tester → Kode**.

## Testing

Prosjektet utvikles med **testdrevet utvikling (TDD)** etter rød–grønn–refaktor-prinsippet:

1. **Enhetstester av domenet:** Tester forretningsregler, validering av organisasjonsnummer og gyldige tilstandsoverganger for tilknytninger og annonser helt uavhengig av database og rammeverk.
2. **Integrasjonstester med Testcontainers:** Kjører alle Flyway-migreringer mot en ekte PostgreSQL 17-container og verifiserer lagring, eierskapsregler, optimistisk låsing og auditspor.
3. **Web- og sikkerhetstester:** Verifiserer SEO-metadata, `sitemap.xml`, `robots.txt`, feilsider, CSRF-beskyttelse ved utlogging og at Vaadin håndhever tilgangskontroll (`@RolesAllowed`) ved navigasjon.

## Utfordringer underveis

- **Fra brukerkonto til B2B-domenemodell:** Første utgave ga enkeltpersoner en fast selgerrolle og personlig eierskap til annonser. Gjennom arbeid med domenemodellen ble dette erstattet av *virksomhet*, *verifisert tilknytning* og *annonsegodkjenning*, slik at annonser tilhører virksomheten og ingenting publiseres eller deles med kollegaer før tilknytningen er verifisert.
- **Balansen mellom domenedrevet og testdrevet arbeid:** Å skrive tester før domenebegrepene er ordentlig avklart låser fast feil begreper i kode og databasetabeller. Løsningen ble å oppdatere brukerflyter, ordliste (`CONTEXT.md`) og domenemodell før nye tester og kode skrives.
- **Skillet mellom åpne sider og Vaadin-app:** Da Vaadin først lå på rotstien (`/*`), oppstod det konflikter med de offentlige Thymeleaf-sidene og svakheter i klientside-ruting. Dette ble løst ved å samle hele Vaadin-appen under `/app/*` og aktivere streng tilgangskontroll ved hver navigasjon.
- **Samspill med automatiske sikkerhetsrettinger:** Aikido Security fanget opp nyttige sårbarheter og konfigurasjonsfeil underveis, men automatiske kodeendringer fjernet også innstillinger som den lokale demoen trengte. Sikkerhetsendringer måtte derfor tilpasses slik at både sikkerhetskrav og lokal kjøring fungerer sammen.

## Mulig videreutvikling

Veien videre fra prototype til ferdig MVP er kartlagt i egne utredninger (`docs/research/`) og brukerflyter:

- **Bilder og PDF-dokumentasjon:** Sikker opplasting med innholdskontroll (Apache Tika), omkoding av bilder til ny JPEG (fjerner metadata/EXIF) og rensing av PDF-dokumenter (Apache PDFBox).
- **Bud, handel og handelsverifisering:** La virksomheter gi bud på publiserte annonser, sette andre bud på vent når et bud aksepteres, og la markedsplassadministratoren verifisere handelen før gjennomføring.
- **Lagrede annonser og varsling:** La brukere lagre annonser for sammenligning, og sende varsler i appen og på e-post når en tilknytning, annonse eller handel endrer status.
- **Oppslag mot Enhetsregisteret:** Hente navn og organisasjonsform automatisk fra Brønnøysundregistrenes åpne API ved registrering av organisasjonsnummer.
- **Opprydding og produksjonsdrift:** Fjerne siste rester av den utgående selgersøknadsmodellen fra koden, ferdigstille slettefrister for personopplysninger, og sette opp produksjonsmiljø hos europeisk skyleverandør.

---

## Kjør prosjektet lokalt

Start PostgreSQL, Keycloak og applikasjonen med demoskriptet:

```bash
scripts/demo.sh up
```

- Offentlig markedsplass: `http://localhost:8080/utstyr`
- Innlogget arbeidsflate (Min side): `http://localhost:8080/app`
- Administrasjon: `http://localhost:8080/app/admin`

Kjør alle tester (krever Docker for Testcontainers):

```bash
./mvnw test
```

## Dokumentasjon i repoet

- [`CONTEXT.md`](CONTEXT.md) – Ordliste med faste domenebegreper
- [`docs/domain/domenemodell.md`](docs/domain/domenemodell.md) – Klassediagram, tilstandsdiagrammer og personvernoversikt
- [`docs/design/README.md`](docs/design/README.md) – Oversikt over designmetode og brukerflyter (`docs/userflows/`)
- [`docs/decisions/`](docs/decisions/) – Arkitektur- og teknologibeslutninger (`0001`–`0010`)
- [`docs/research/`](docs/research/) – Utredninger om innlogging (`R1`), filer (`R2`), offentlige registre (`R3`) og drift (`R4`)
- [`docs/local-development.md`](docs/local-development.md) – Detaljert guide for lokal demo, testbrukere og feilsøking
