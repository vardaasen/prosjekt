# Feilmeldinger og hjelpetekster i skjema

Konvensjon for alle skjema, offentlige sider og app. Gjelder sammen med
beslutningsnotat 0008 (WCAG 2.2 AA ufravikelig, AAA som mål) og
designhåndbokens krav om «synlige etiketter og forståelige feilmeldinger».

## Regler

1. **Si hva som er galt og hvordan det rettes.** En melding som bare sier at
   noe er feil, er ikke nok. [GOV.UK] [WCAG 3.3.3]
2. **Ulike feil, ulike meldinger.** Tomt felt, feil format og feil innhold får
   hver sin melding. [GOV.UK]
3. **Tomt påkrevd felt: instruks.** «Skriv inn …» eller «Velg …», med samme
   ord som etiketten. [GOV.UK]
4. **Brutt regel: beskrivelse.** «X må være …» eller «Sjekk at …». [GOV.UK]
5. **Unngå** «ugyldig», «gyldig», «feil», «vennligst», «beklager», tekniske
   koder og humor. Unngå meldinger som «Feltet er påkrevd» eller «Det oppstod
   en feil». [GOV.UK]
6. **Plassering:** meldingen står ved feltet og er programmatisk koblet til
   det, slik at skjermlesere leser den. Vaadins felt gjør dette når feilen
   settes med `Binder`. [WCAG 3.3.1]
7. **Behold det brukeren skrev**, slik at det kan rettes uten å skrives på
   nytt. [GOV.UK]
8. **Påkrevde felt merkes synlig og programmatisk** før feil oppstår, og
   format forklares i hjelpeteksten (for eksempel «Ni sifre, for eksempel
   974 760 673»). [WCAG 3.3.2]
9. **Lange skjema** får i tillegg en feiloppsummering øverst med de samme
   meldingene. [GOV.UK]
10. **Les meldingen høyt.** Den skal høres ut som noe en person ville sagt.
    [GOV.UK]

## Eksempler i løsningen

| Felt | Situasjon | Melding |
| --- | --- | --- |
| Organisasjonsnummer | tomt | Skriv inn organisasjonsnummeret. |
| Organisasjonsnummer | ikke ni sifre | Organisasjonsnummeret må være ni sifre. |
| Organisasjonsnummer | kontrollsifferet stemmer ikke | Sjekk at organisasjonsnummeret er skrevet riktig. |
| Navn på virksomheten | tomt | Skriv inn navnet på virksomheten. |
| Hele siden (`/okt-utlopt`) | handling sendt med utløpt økt, for eksempel «Logg ut» etter omstart | Økten din var utløpt. Handlingen ble ikke utført, fordi økten din hadde utløpt. Hvis du prøvde å logge ut, er du allerede logget ut av Havbruksbrukt. |
| Hele siden (`error.html`) | uventet feil | Noe gikk galt. Siden kunne ikke vises akkurat nå. Prøv igjen om litt, eller gå tilbake til markedsplassen. |

Nye meldinger legges inn i tabellen når de innføres, og testes der det er
praktisk (se `OrganisationNumberMessagesTest`).

## Kilder

- [GOV.UK] GOV.UK Design System, «Error message»:
  https://design-system.service.gov.uk/components/error-message/
- [WCAG 3.3.1] Error Identification (A):
  https://www.w3.org/WAI/WCAG22/Understanding/error-identification.html
- [WCAG 3.3.2] Labels or Instructions (A):
  https://www.w3.org/WAI/WCAG22/Understanding/labels-or-instructions.html
- [WCAG 3.3.3] Error Suggestion (AA):
  https://www.w3.org/WAI/WCAG22/Understanding/error-suggestion.html
- Designhåndboken, «Tilgjengelighet» og «Interaksjon» (`DesignMarketPlace/`).
