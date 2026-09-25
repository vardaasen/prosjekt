# 0007: Verifisert tilknytning og verifisert handel i stedet for selgerrolle

## Status

Akseptert 2026-09-24. Erstatter selgerdelen av 0006 (selgersøknad og
varig `SELLER`-rolle). Skillet mellom Keycloak-plattformadministrator og
markedsplassadministrator fra 0006 gjelder fortsatt.

## Problem

0006 lot en person søke om å bli selger og ga en varig `SELLER`-rolle ved
godkjenning. Det passer ikke med hvordan B2B-handel faktisk skjer:

- Kjøp og salg er ikke kontotyper. Den samme virksomheten kjøper i én handel
  og selger i en annen.
- Partene er virksomheter, men det er personer som logger inn, og en person
  kan handle for flere virksomheter.
- En verifisering som varer for alltid gir falsk trygghet. Personer slutter,
  og fullmakter endres.
- Pengene flyttes fysisk mellom partene. Markedsplassen trenger nulltillit ved
  selve handelen, ikke bare ved oppstart.

## Alternativer

- Beholde 0006: selgersøknad og varige roller for kjøper og selger.
- Personen som part, med virksomheten som profilopplysning.
- Virksomheten som part, med verifisert tilknytning mellom person og
  virksomhet, og administratorverifisering av annonser og handler.

## Valg

Virksomheten er part i annonser, bud og handler. En person handler for en
virksomhet gjennom en tilknytning som markedsplassadministratoren verifiserer.
Selger og kjøper er roller i en bestemt annonse eller handel.

- Innlogget person uten verifisert tilknytning kan registrere en virksomhet
  og gi bud. Tilknytningen opprettes første gang den trengs; det finnes ikke
  noe eget søknadssteg. (Utkast krever verifisert tilknytning, se tillegg.)
- Ingenting blir synlig på markedsplassen før verifisering. En annonse krever
  verifisert tilknytning og annonsegodkjenning før publisering.
- Hver handel verifiseres av markedsplassadministratoren før den regnes som
  gjennomført. Markedsplassen håndterer ikke betalingen.
- Foreløpig slutter en verifisert tilknytning å gjelde bare når
  administratoren trekker den tilbake.

## Konsekvens

- Keycloak-rollene `SELLER` og `BUYER` fases ut; `ADMIN` beholdes som
  markedsplassrolle. Tilgang avgjøres av tilknytninger i markedsplassens
  database, ikke av realm-roller.
- Koden bygget på 0006 (`SellerApplication`, `SellerAccount`,
  rolletildeling via Keycloak Admin API) må migreres. Det bør planlegges som
  egne skiver, ikke som én stor omskriving.
- Userflow 7 og 10 må revideres.
- Åpne spørsmål, ikke besluttet:
  - Automatisk kontroll av tilknytninger mellom verifiseringer, blant annet
    mot offentlige registre. Virksomheter kan hentes fra Enhetsregisteret
    (åpent API, egen beslutning om ekstern tjeneste). Kobling mellom person og
    virksomhet er vanskeligere å kontrollere mot offentlige kilder.
  - Utløp ved inaktivitet (sovende tilknytninger).
  - Personvern: sletting av personer og tilknytninger som ikke lenger trengs.
  - Lovpålagt oppbevaring av handler (for eksempel bokføringsregler) og hva
    som skal slettes når.
  - Om verifisering kan gjøres sømløs nok til at virksomheter ikke bremses.

## Tillegg: utkast krever verifisert tilknytning (2026-09-25)

Manuell test av #19/#22 viste at utkast laget før verifisering havnet hos
kollegaene i virksomheten, og at hvem som helst som registrerte et
organisasjonsnummer fikk innsyn i virksomhetens utkast. Produkteier besluttet
den enkleste regelen: **et utkast kan bare lages, endres og ses med verifisert
tilknytning**. Bare utkast laget av verifiserte personer havner hos kollegaene.
Et utkast laget av en person som senere mister verifiseringen, kan sendes inn
av en verifisert kollega etter en advarsel og en bekreftelse som står i
auditsporet. Bud før verifisering er uendret.
