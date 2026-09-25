# R3: Offentlige registre, informasjonsmodeller og autoritative kilder

Status: **Produkteiers notater**, 2026-09-25, strukturert av agent. Lenkene er
registrert slik produkteier fant dem; påstander er ikke etterprøvd på nytt.
Underlag for #27 (Enhetsregisteret), #28 (personvern) og domenemodellen.

## Sammendrag

- Det finnes ingen enkel, samlet og oppdatert autoritativ definisjon av
  begreper som organisasjonsnummer i Digdirs referansesider eller på
  data.norge.no. Myndighetene leverer i stor grad data som strenger gjennom
  API-er, og definisjonen må hentes fra den enkelte registereiers
  dokumentasjon.
- Oppslag i registre bak Maskinporten er ikke nøytrale. De logges og gir spor
  og varsler hos den det søkes på.
- Virksomhetsdata fra Enhetsregisterets åpne datasett kan brukes uten
  registrering. Oppslag på roller med fødselsnummer krever søknad i
  Maskinporten.
- Digdir omtaler «Enhet» som et samlebegrep for blant annet juridisk person,
  virksomhet/bedrift og offentlige organisasjonsledd. Det påvirker hva
  «virksomhet» betyr i `CONTEXT.md` (se åpne spørsmål).

## Informasjonsmodeller hos Digdir

- *Person og enhet – felles informasjonsmodell*:
  https://www.digdir.no/informasjonsforvaltning/person-og-enhet-felles-informasjonsmodell/2018
  Siden virker delvis aktiv (for eksempel en definisjon som kan brukes til
  mønstergjenkjenning av organisasjonsnummer), men lenker til RFC-er peker til
  rfc-editor.org i stedet for https://datatracker.ietf.org/doc/. Den sier om
  «8. Enhet» at begrepet er et samlebegrep, og at en grundig gjennomgang var
  «planlagt for 2017». Siden brukes trolig som et veiledende dokument.
- Modellen på data.norge.no mangler detaljer (for eksempel om
  organisasjonsnummer) og kan være forlatt:
  https://data.norge.no/information-models/409c97dd-57e0-3a29-b5a3-023733cf5064
- Søk etter informasjonsmodeller: https://data.norge.no/information-models og
  dokumentasjon https://data.norge.no/nb/docs/catalogs/information-models
  (forsiden har også AI-søk).
- Referansemodeller og EU Core Vocabularies:
  https://www.digdir.no/informasjonsforvaltning/referansemodeller-innen-informasjonsforvaltning/2156
- ModellDCAT-AP-NO, hvordan informasjonsmodeller beskrives:
  - Veileder 1.0.1 (støtter bare spesifikasjon 1.1):
    https://data.norge.no/guide/veileder-modelldcat-ap-no
  - Versjon 1.3: https://data.norge.no/information-models/45cf62bf-1863-3cc1-8eaf-59bf1e052f09
  - Gjeldende spesifikasjon 1.4.0:
    https://data.norge.no/specification/modelldcat-ap-no#Spesifikasjon-per-klasse
  - Klassediagrammet viser blant annet Informasjonsmodell, Modellelement
    (Objekttype, Datatype, Enkeltype, Kodeliste, Modul), Egenskap (Attributt,
    Assosiasjon, Rolle, Spesialisering, Komposisjon med flere) og
    Begrensningsregel. I diagrammet produkteier fant, er «Attributt» merket
    `modelldcatno:Dependency`, som trolig skal være `modelldcatno:Attribute`.
- Det er uklart om det offentlige har gått bort fra RDF, og hvor en slik
  beslutning i så fall står.

## Autoritative kilder for identifikatorer

- Folkeregisteret (Skatteetaten), informasjonsmodell og API:
  - https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/informasjonsmodell/
  - https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/endepunkter/
  - Test for konsumenter, nevner modulus 11:
    https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/test-for-konsumenter/
  - Informasjonsmodell v4.40 med definisjon av organisasjonsnummer:
    https://skatteetaten.github.io/folkeregisteret-api-dokumentasjon/dokumenter/2023_10_16_Informasjonsmodell-Modernisering%20av%20folkeregisteret%20v4.40.pdf
- Brønnøysundregistrene, Enhetsregisteret API (inkludert roller):
  https://data.brreg.no/enhetsregisteret/api/dokumentasjon/no/index.html#tag/Roller
- Hvis registereierne bare leverer strenger gjennom API-er og ikke definerer
  formatet i tekniske dokumenter, finnes det ikke en definisjon å bruke som
  utgangspunkt for mønstergjenkjenning. Kontrollen i koden må da vise til den
  kilden som faktisk beskriver regelen.

## Tilgang: åpne data og Maskinporten

- Brønnøysundregistrene: «Det er ikke nødvendig å registrere seg for å ta
  datasettet i bruk.» https://www.brreg.no/bruke-data-fra-bronnoysundregistrene/datasett-og-api/
- Oppslag på roller med fødselsnummer
  (`brreg:data:enhetsregisteret:roller:person:oppslag:fnr`) krever søknad i
  Maskinporten, som Folkeregisteret.
- Maskinporten for konsumenter krever organisasjonsnummer:
  https://samarbeid.digdir.no/maskinporten/konsument/119. Hva det innebærer å
  bruke et organisasjonsnummer, og om alle kan brukes, er uklart.
- Server-til-server OAuth2: https://docs.digdir.no/docs/Maskinporten/maskinporten_auth_server-to-server-oauth2.html
- Egen nøkkel i stedet for virksomhetssertifikat:
  https://docs.digdir.no/docs/Maskinporten/maskinporten_sjolvbetjening_api.html#registrere-klient-som-bruker-egen-nøkkel
- Digital post (DPI): https://docs.digdir.no/dpi_nyinfrastruktur.html
- ELMA for EHF-integrasjon: https://samarbeid.digdir.no/elma/elma/24

## Adresser

- Kartverket/Matrikkelen: https://ws.geonorge.no/adresser/v1/ og
  https://www.kartverket.no/api-og-data/eiendomsdata/brukarrettleiing-adresse-api
- Posten/Bring: https://developer.bring.com/api/address/ og
  https://developer.bring.com/api/postal-code/

## Personvern: oppslag etterlater spor

Skatteetaten om Folkeregisteret: hver person har rett til opplysninger om søk
på seg selv, og alle søk logges med konsumentens navn, organisasjonsnummer,
adresse og tidspunkt. Oppslag i registre bak Maskinporten er dermed «harde
søk» som lager offentlige spor, og den det søkes på kan få melding. Det finnes
ingen samlet oversikt over hvilke leverandører som gjør slike oppslag.

**Konsekvens for Havbruksbrukt (0009):** slå opp virksomheter i åpne data, aldri
personer. Oppslag på roller med fødselsnummer brukes ikke uten eget
beslutningsnotat, behandlingsgrunnlag og vurdering av sporene det etterlater.

## Åpne spørsmål

- **Virksomhet eller enhet?** I Enhetsregisteret har en hovedenhet (juridisk
  enhet) og hver underenhet (bedrift) egne organisasjonsnummer. `CONTEXT.md`
  definerer virksomhet som «den juridiske enheten … identifisert med
  organisasjonsnummer». Hvilken av dem er part i en handel, og hvilken
  registrerer en person på Min side? (#27)
- Hvilken kilde skal organisasjonsnummer-kontrollen i koden vise til?
- Er ModellDCAT-AP-NO aktuell for å beskrive domenemodellen, for eksempel for
  publisering på data.norge.no?
