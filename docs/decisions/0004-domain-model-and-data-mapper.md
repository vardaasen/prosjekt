# 0004: Domeneobjekter uten persistens og Data Mapper ved databasekanten

## Status

Akseptert for neste implementasjonsfase.

## Problem

Markedsplassen går fra in-memory prototyping til persistens. Dagens domene
skal kunne uttrykke annonser, selgere, tilstand og publiseringsregler uten at
Spring Data JPA, Hibernate eller tabellstruktur sprer seg inn i offentlige
ruter, Vaadin-visninger eller domeneregler.

## Alternativer

- Annotere domeneobjektene direkte som JPA-entiteter og bruke Spring Data
  repositories i tjenester og visninger.
- Innføre en separat persistence-adapter med JPA-entiteter og en Data Mapper
  mellom persistensmodellen og domenemodellen.
- Beholde in-memory data til hele selgerflyten er ferdig definert.

## Valg

Vi beholder domeneobjekter uten JPA-annotasjoner. En katalog-seam definerer
bare de nåværende leseoperasjonene for publiserte annonser. In-memory data og
senere JPA/PostgreSQL er adaptere ved denne seamen.

JPA-entiteter er interne for persistence-adapteren. En dedikert Data Mapper
oversetter mellom JPA-entitetene og domenemodellen. Spring Data-repositories
eksponeres ikke utenfor adapteren.

## Konsekvenser

- Offentlig katalog, annonsedetalj og sitemap bruker bare publiserte annonser.
- Første persistensskive introduserer `DRAFT`, `PUBLISHED` og `ARCHIVED`, med
  Flyway-migreringer og PostgreSQL-baserte integrasjonstester.
- Det oppstår noe eksplisitt mapping-kode, men dette beskytter domenespråket
  mot schema- og ORM-detaljer.
- Hvis framtidige behov gjør kartleggingen uforholdsmessig enkel eller tung,
  kan valget revurderes før data og eksterne integrasjoner har vokst.
