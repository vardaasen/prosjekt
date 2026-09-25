# 0009: Innebygd sikkerhet og personvern som standard

## Status

Akseptert 2026-09-25. Gjelder alle arkitektur- og teknologivalg.

## Problem

Markedsplassen behandler personopplysninger (personer, tilknytninger til
virksomheter, bud og handler) og filer fra brukere. Personvernforordningen
artikkel 25 krever innebygd personvern og personvern som standard. Tidligere
arbeid viste at sikkerhet lagt på i etterkant ble til workarounds som selv
åpnet hull (CSRF-unntak, uhåndhevet tilgangskontroll).

## Valg

Innebygd sikkerhet og personvern som standard er et grunnprinsipp som veier
tyngre enn bekvemmelighet ved alle arkitektur- og teknologivalg:

- **Dataminimering:** samle og lagre bare det en konkret funksjon trenger.
  Ingenting lagres før det trengs, for eksempel ingen utkast eller filer før
  innlogging.
- **Lagringstid:** hver type personopplysning har en bestemt lagringstid og
  slettes når den ikke lenger trengs, med unntak av lovpålagt oppbevaring.
- **Sikre standarder:** ny funksjonalitet er lukket til den åpnes eksplisitt
  (minste privilegium, tilgangskontroll på hver navigasjon, CSRF på).
- **Rammeverkets egne mekanismer** foretrekkes framfor hjemmelagde varianter
  av sikkerhet (for eksempel Vaadin/Spring Security).
- **Enkelt framfor smart:** den enkleste løsningen som oppfyller kravene
  velges, fordi den er lettest å forstå, teste og drifte sikkert.
- **Eksterne tjenester** (e-post, identitetsleverandører, filvalidering)
  krever eget beslutningsnotat som beskriver hvilke personopplysninger som
  sendes, hvor de behandles og hvilken lisens som gjelder.

## Konsekvens

- `intent-grounding` sjekker valg mot dette prinsippet.
- Research- og beslutningsnotater om e-post, innlogging og filer må svare på
  dataminimering, lagringstid, behandlingssted og lisens.
