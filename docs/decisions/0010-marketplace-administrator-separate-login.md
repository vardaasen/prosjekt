# 0010: Markedsplassadministrator er en egen innlogging som ikke handler

## Status

Akseptert 2026-09-25. Utfyller 0006 (skillet mellom Keycloak-plattform-
administrator og markedsplassadministrator) og 0007 (administratoren
verifiserer tilknytninger, annonser og handler).

## Problem

Markedsplassadministratoren verifiserer tilknytninger, godkjenner annonser og
verifiserer handler. Hvis den samme innloggingen også kunne registrere en
virksomhet, lage annonser eller gi bud, kunne administratoren verifisere sine
egne tilknytninger, annonser og handler.

## Alternativer

- Egen innlogging for administratoren, som aldri handler.
- Samme innlogging, med regler som hindrer at man verifiserer sitt eget og krav
  om minst to administratorer.
- Administratoren kan se den offentlige flaten, men ikke handle.

## Valg

Markedsplassadministratoren er en egen innlogging som aldri handler og ikke
surfer (arbeidsdeling). Den lander på Administrasjon og har bare
Administrasjon og Logg ut i menyene. Den har ikke Min side og kan ikke
registrere virksomhet, lage annonser eller gi bud. En person som både
administrerer og handler, bruker to innlogginger.

## Konsekvens

- Menyene (userflow 10) skiller mellom markedsplassadministrator og andre
  innloggede personer.
- Min side sender markedsplassadministratoren videre til Administrasjon.
- Senere funksjoner (tilknytning, utkast, bud) skal avvise
  markedsplassadministratoren på serveren, ikke bare skjule knapper.
