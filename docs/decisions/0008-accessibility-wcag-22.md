# 0008: Universell utforming etter WCAG 2.2

## Status

Akseptert 2026-09-24.

## Problem

Markedsplassen skal kunne brukes av alle i bransjen, på offentlige sider og i
appen. Manuell test viste skjemaer der obligatoriske felt ikke var merket og
feil ikke ble forklart, som bryter WCAG allerede på nivå A (3.3.2 Ledetekster
eller instruksjoner). W3C anbefaler ikke AAA som generelt krav for hele
nettsteder, fordi enkelte kriterier ikke kan oppfylles for alt innhold.

## Alternativer

- WCAG 2.2 AA som krav.
- WCAG 2.2 AAA som ufravikelig krav overalt.
- WCAG 2.2 AA som ufravikelig krav, AAA som mål der det er praktisk mulig.

## Valg

WCAG 2.2 AA er et ufravikelig krav for alle offentlige sider og hele appen.
AAA er målet der det er praktisk mulig, blant annet kontrast 7:1, tydelige
hjelpe- og feiltekster, ingen tidsgrenser og konsistent navigasjon. Hvert
avvik fra AAA dokumenteres med begrunnelse.

## Konsekvens

- Obligatoriske felt merkes synlig og programmatisk, og feil forklares ved
  feltet med forslag til retting.
- Nye skjemaer og sider testes med tastatur og skjermleser, og med
  automatiske kontroller i testene der det er mulig.
- Fargepaletten i designhåndboken må kontrolleres mot 7:1 for tekst.
- Eksisterende skjemaer (utkast, selgersøknad, filter) må gjennomgås.
