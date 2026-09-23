# 0003: MCP-servere for Vaadin og Figma

## Problem

Prosjektet trenger pålitelige kilder for Vaadin-versjon/API-valg og en mulig bro til designmateriale når Figma blir kilde for flyter eller skjermbilder. `.vscode/mcp.json` inneholder nå Vaadin og Figma MCP-servere.

## Alternativer

- Bruke bare generell modellkunnskap.
- Bruke Vaadin MCP for dokumentasjon/API og holde Figma MCP tilgjengelig ved designarbeid.
- Legge til flere diagram-/SEO-MCP-er fra start, for eksempel Miro, Lucid eller SEO-verktøy.

## Valg

Behold Vaadin MCP og Figma MCP i `.vscode/mcp.json`.

- **Vaadin MCP trengs nå** for versjonsriktig Vaadin 25.2-/Flow-/komponentdokumentasjon, særlig fordi prosjektet bruker Java 24, Spring Boot 4.1+ og åpen-komponent-strategi.
- **Figma MCP trengs når Figma-filer blir kilde** for komponenter, userflows, skjermbilder eller design tokens. Uten en konkret Figma-fil/lenke brukes eksisterende designmateriale og Markdown/Mermaid som arbeidsformat.
- **Ekstra diagram-MCP trengs ikke nå.** Mermaid i repoet gir versjonerte, reviewbare userflows. Miro/Lucid kan vurderes senere hvis diagrammene må redigeres visuelt av flere.
- **SEO-MCP trengs ikke før site/domene finnes.** VibeSEO eller Google Search Console MCP vurderes når offentlig staging/produksjon er tilgjengelig og tilgang kan gis trygt.

## Konsekvens

MCP-oppsettet er lett og relevant for nåværende arbeid. Nye MCP-servere skal fortsatt kreve eksplisitt valg og beslutningsnotat hvis de kobles til kontoer, eksterne tjenester eller data.
