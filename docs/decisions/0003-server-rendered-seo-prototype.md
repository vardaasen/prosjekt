# 0003: Server-rendered SEO-prototype

## Status

Akseptert for prototypefasen.

## Beslutning

Den offentlige SEO-flaten bruker Spring MVC og Thymeleaf for server-rendered HTML. Canonical URL, robots-policy, sitemap og 404-respons testes gjennom HTTP-responsen. Vaadin Flow beholdes for `/app`.

## Begrunnelse

- Søkemotorer får innhold, metadata og interne lenker uten å kjøre en klient.
- Filterkombinasjoner kan deles, men får `noindex,follow` og peker canonical til `/utstyr` for å unngå tynne duplikatsider.
- Sitemap inneholder bare forsiden, katalogen og publiserte annonser fra den offentlige tjenesten.
- Prototypen kobler ikke til eksterne SEO-verktøy og bruker eksisterende in-memory data.

## Konsekvens

Når listingdata flyttes til database, må sitemap-generering og publiseringsstatus fortsatt begrenses til komplette, publiserte annonser. En egen base-URL-konfigurasjon kan innføres dersom appen står bak en proxy som ikke videresender request-host korrekt.
