# 0001: Åpne komponenter som standard i Vaadin

## Problem

Prosjektet skal måles opp og videreutvikles som lærings-/prototypeprosjekt, ikke kommersialiseres. Vaadin har både åpne Core-komponenter og kommersielle Enterprise-komponenter.

## Alternativer

- Bruke `vaadin-spring-boot-starter` og åpne Vaadin Core-komponenter som standard.
- Legge til `com.vaadin:vaadin` fra start for tilgang til kommersielle komponenter.
- Bruke tredjeparts eller egenutviklede komponenter der Core ikke dekker behovet.

## Valg

Bruk åpne Vaadin Core-komponenter som standard. Ikke legg til kommersielle Vaadin-avhengigheter uten eget beslutningsnotat.

## Konsekvens

Prosjektet holder seg enkelt å kjøre uten lisens. Hvis et Enterprise-valg blir aktuelt, må notatet forklare behovet, åpne alternativer og lisenskonsekvens.
