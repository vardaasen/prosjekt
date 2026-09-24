# 0006: Administrator og godkjenning av selgere

## Status

Akseptert som sikkerhetsgrense før selvbetjent registrering eller
administratorflate implementeres.

## Problem

Markedsplassen trenger en kontrollert vei fra en ny OIDC-identitet til en
selger som kan publisere annonser. Å la selvregistrering tildele `SELLER`
direkte vil gjøre det mulig for enhver bekreftet konto å publisere i en
B2B-markedsplass. Samtidig er Keycloaks plattformadministrator nødvendig for
å drifte identitetsleverandøren, men skal ikke være markedsplassens daglige
forretningsadministrator.

## Valg

Vi skiller to administrative ansvar:

1. **Keycloak-plattformadministrator** drifter Keycloak, realm, klienter,
   nøkler og gjenoppretting. Kontoen opprettes ved sikker bootstrap gjennom
   deploy-konfigurasjon og brukes bare til IAM-drift.
2. **Markedsplassadministrator** er en vanlig OIDC-identitet med
   `ADMIN`-realmrolle. Den bruker markedsplassens egen `/app/admin`-flate for å
   vurdere selgersøknader og tildele eller trekke tilbake
   markedsplassprivilegier.

Nye brukere kan registrere seg hos Keycloak etter e-postverifisering, men får
ingen `SELLER`- eller `ADMIN`-rolle ved opprettelse. En selgersøknad lagres i
markedsplassens database med status `PENDING`. Bare en innlogget
markedsplassadministrator kan godkjenne søknaden. Godkjenningen tildeler
`SELLER` gjennom en minste-privilegium-integrasjon mot Keycloaks
administrasjons-API og oppretter/aktiverer den tilhørende `SellerAccount`.

Den første markedsplassadministratoren opprettes som en kontrollert
bootstrap-operasjon i hvert miljø: en driftseier oppretter en navngitt OIDC-
bruker, tildeler bare `ADMIN` i Keycloak-admin-konsollet eller med en
engangskommando fra en hemmelighetsforvaltet deploy-prosess, og dokumenterer
utførelsen i miljøets tilgangsregister. Denne bootstrapen finnes ikke i
applikasjonens HTTP-grensesnitt, migreringer eller repository.

## Konsekvenser

- Selvregistrering gir ikke publiseringsrettigheter.
- `ADMIN` er en markedsplassrolle, ikke en Keycloak-administratorrolle.
- Administratorhandlinger krever auditlogg med administrator-subject,
  berørt søknad, beslutning og tidspunkt.
- Keycloak service-kontoen som brukes til rolletildeling skal ha bare
  nødvendige realm-/brukeradministrasjonsrettigheter, med klienthemmelighet
  i ekstern hemmelighetsforvaltning.
- Før registrering aktiveres må appen ha rate limiting, e-postverifisering,
  søknadsvalidering, CSRF-beskyttelse og en testet godkjenningsflyt.

