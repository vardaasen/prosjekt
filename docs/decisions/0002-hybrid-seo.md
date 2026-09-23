# 0002: Hybrid SEO-arkitektur

## Problem

Markedsplassen trenger offentlige sider som søkemotorer kan lese, samtidig som selger- og arbeidsflyter bør bygges effektivt i Java med Vaadin Flow.

## Alternativer

- Kun Vaadin Flow-ruter for alt innhold.
- Bespoke statisk HTML for offentlige sider og Vaadin bak innlogging.
- Server-rendered Java/Spring-sider for offentlig innhold og Vaadin Flow for appflater.

## Valg

Bruk server-rendered Java/Spring-sider for offentlig forside, katalog og annonsedetaljer. Bruk Vaadin Flow for innloggede og interaktive appflater.

## Konsekvens

SEO-relevant HTML og metadata er tilgjengelig uten at søkemotoren må kjøre Vaadin-klienten. Mockup-HTML fra designfasen kan brukes som referanse, men skal ikke kopieres ukritisk.
