# Design: kart og metode

Designet dokumenteres som én kjede. Hvert ledd bygger på leddet over, og
hvert valg skal kunne spores tilbake.

| Ledd | Hvor | Hva | Bygger på |
| --- | --- | --- | --- |
| 1. Userflows | `docs/userflows/` | Hvordan personer beveger seg gjennom løsningen, steg for steg. Nummererte flyter, én Mermaid-kilde per flyt i `diagrams/`. | Designhåndboken og brukerbehov |
| 2. Domenemodell | `docs/domain/domenemodell.md` | Klassediagram, tilstandsdiagrammer og personopplysninger per entitet. Hver klasse og overgang peker til et steg i en userflow. | Userflows |
| 3. Ordliste | `CONTEXT.md` | Presise begreper, uten implementasjon. | Userflows og domenemodell |
| 4. Beslutninger | `docs/decisions/` | Valg som er vanskelige å reversere, med alternativer og konsekvenser. | Alle ledd |
| Research | `docs/research/` | Funn fra primærkilder som underlag for beslutninger. | Åpne spørsmål i leddene over |

## Metode

1. **Userflow først.** Ny funksjonalitet starter med en userflow: hvem gjør
   hva, i hvilken rekkefølge, og hva som kan gå galt. Åpne spørsmål merkes i
   flyten.
2. **Domenemodell fra flytene.** Substantiver og verb i flytene gir
   entiteter, relasjoner og operasjoner (skillen `domain-model-diagram`).
   Statusendringer i flytene gir tilstandsdiagrammene.
3. **Grilling av det uklare.** Åpne spørsmål avklares ett om gangen
   (`grilling`). Svaret oppdaterer flyten først, deretter modellen.
4. **Begreper og beslutninger** oppdateres fortløpende (`domain-modeling`).
5. **Kode** skrives først når leddene over er konsistente
   (`intent-grounding` sjekker det).

## Userflows

| Nr. | Flyt | Status |
| --- | --- | --- |
| 1–5 | Offentlig sitemap, kjøper, filtrering, annonsedetalj, selger fra offentlig side | Tidlig fase, delvis implementert |
| 6 | Selger lager utkast | Implementert (gammel modell) |
| 7 | Administrator godkjenner selgersøknad | Implementert, **erstattet av 0007** |
| 8–9 | Drift og lokal demo | Delvis |
| 10 | Navigasjon, innlogging og utlogging | Besluttet, trinn 1 implementert |
| 11 | Tilknytning til virksomhet | Utkast (0007) |
| 12 | Annonse fra utkast til publisert | Utkast (0007) |
| 13 | Bud | Utkast (0007), åpne spørsmål |
| 14 | Handel og handelsverifisering | Utkast (0007) |

Oversikt og diagrammer: `docs/userflows/seo-public-app-diagrams.md`
(filnavnet er historisk; filen samler alle flyter). Detaljer for flyt 10:
`docs/userflows/navigasjon-og-innlogging.md`.

## Historikk

Tidlige designartefakter beholdes som referanse, ikke som gjeldende design:
designhåndboken, wireframe og mockups i `DesignMarketPlace/` (utenfor Git),
PDF-ene i rotmappen og userflow 1–5 slik de ble tegnet i tidlig fase.
Designhåndbokens visuelle retning, tone og tilgjengelighetsregler gjelder
fortsatt.
