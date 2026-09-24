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

## Agent skills

Prosjektspesifikke skills ligger i `.github/skills/` (kilden, brukt av
Copilot). Claude Code leser en generert kopi i `.claude/skills/`: rediger
alltid `.github/skills/` og kjør `scripts/sync-claude-skills.sh`. I Claude
Code heter `code-review` `two-axis-review`, fordi Claude Code har en innebygd
`/code-review`. Flere Matt Pocock-skills er tilpasset prosjektet
(`docs/decisions/`, `docs/agents/`), så hashene i `skills-lock.json` viser
opprinnelsen, ikke nåværende innhold.

Bruk `intent-grounding` før enhver funksjonell endring eller feilretting:
den krever en kort begrunnelse mot `CONTEXT.md`, userflows og
beslutningsnotater før kode, og stopper fiks-løkker der workarounds stables
oppå et valg som strider mot arkitekturen.

Matt Pocock-settet er bevisst kuratert til arbeidsflyter prosjektet bruker; de øvrige skillene
dekker Java, Vaadin, SEO og database. Provenans for det kuraterte settet ligger
i `skills-lock.json`.

`grilling` er lagt til lokalt fordi `grill-with-docs` og `triage` bygger på
den.

For domene-/persistensfasen brukes særlig `domain-modeling`,
`codebase-design`, `tdd`, `effective-java`, `database`, `spring-data-jpa` og
`postgres-best-practices`; Vaadin- og SEO-skills brukes når deres flater
endres.

### Issue tracker

GitHub Issues is the repo's issue tracker. See `docs/agents/issue-tracker.md`.

### Triage labels

Default repo triage labels are `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context repo layout. See `docs/agents/domain.md`.

## Fallgruver

- Ikke legg `DesignMarketPlace/` inn i Git.
- Ikke legg kommersielle Vaadin-avhengigheter inn uten beslutningsnotat.
- Ikke flytt offentlige SEO-sider inn i rene Vaadin-klientruter uten crawlbarhetsbegrunnelse.
- Ikke commit bruker-spesifikke IntelliJ-filer fra `.idea/`.
