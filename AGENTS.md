# AGENTS.md — Prosjekt

B2B-markedsplass for brukt akvakulturutstyr. Første fase ligger i `DesignMarketPlace/` som ignorert designreferanse og skal ikke spores i Git.

## Obligatorisk utviklingssjekkliste

- [ ] Skriv eller oppdater test først for funksjonelle endringer.
- [ ] `mvn test` passerer før arbeid markeres ferdig.
- [ ] `mvn verify` brukes før større leveranser eller release-kandidater.
- [ ] Beslutningsnotat oppdateres når et teknologivalg påvirker lisens, arkitektur, SEO, database eller eksterne tjenester.

## Kommandoer

Java 24 · Maven · IntelliJ IDEA som primær IDE.

- Kjør app: `mvn spring-boot:run`
- Tester: `mvn test`
- Full verifisering: `mvn verify`
- Lokal URL: http://localhost:8080

## Arkitektur

| Område | Regel |
| --- | --- |
| Offentlig SEO-flate | Server-rendered Java/Spring/Thymeleaf-ruter for forside, katalog og annonsedetaljer |
| Appflate | Vaadin Flow-ruter for innloggede/interaktive arbeidsflater |
| UI-komponenter | Vaadin Core og åpne komponenter er standard |
| Enterprise/betalte valg | Ikke standard; krever beslutningsnotat med åpne alternativer |
| Domene | Java records/classes i feature-pakker, testet først |
| Designfase | `DesignMarketPlace/` er referanse, ikke produksjonskode |

## TDD-konvensjon

Bruk rød-grønn-refaktor:

1. Skriv en test som beskriver ønsket oppførsel.
2. Implementer minste kode som får testen til å passere.
3. Refaktorer uten å endre oppførsel.

Foretrukne skills: generell TDD, Spring Boot Testing og Java Junit. Agentene i `.github/agents/` er tilpasset Maven/JUnit.

## Skills

Prosjektspesifikke skills ligger i `.github/skills/`:

- `project-setup` — gjenta/utvide denne initieringen.
- `java-development` — Java 24, Spring Boot og TDD-regler.
- `vaadin-open-first` — Vaadin Flow med åpne komponenter som standard.
- `database` — databasevalg, migreringer og teststrategi.
- `frontend-seo` — server-rendered SEO, tilgjengelighet og designmigrering.

## Fallgruver

- Ikke legg `DesignMarketPlace/` inn i Git.
- Ikke legg kommersielle Vaadin-avhengigheter inn uten beslutningsnotat.
- Ikke flytt offentlige SEO-sider inn i rene Vaadin-klientruter uten crawlbarhetsbegrunnelse.
- Ikke commit bruker-spesifikke IntelliJ-filer fra `.idea/`.
