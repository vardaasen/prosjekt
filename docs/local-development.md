# Lokal utvikling

## Markedsplass med Keycloak

Start lokal PostgreSQL og Keycloak:

```bash
docker compose up -d
```

Vent til Keycloak er klar på `http://localhost:8180`, og start deretter
markedsplassen:

```bash
./mvnw spring-boot:run
```

`http://localhost:8080/utstyr` er offentlig. `http://localhost:8080/app`
videresender anonyme brukere til Keycloak. Logg inn med den lokale
demo-selgeren:

| Felt | Verdi |
| --- | --- |
| Brukernavn | `seller-demo` |
| Passord | `seller-demo-password` |

`buyer-demo` har bare rollen `BUYER` og får ikke tilgang til `/app`. Disse
brukerne og administratorpassordets Compose-standardverdi er kun for lokal
utvikling. De må ikke brukes eller importeres til staging eller produksjon.

## Produksjonsgrenser

Compose-filen kjører Keycloak med `start-dev` og en lokal, ukryptert
utviklingsdatabase. Produksjon må kjøre Keycloak separat med HTTPS, sikker
hemmelighetsforvaltning, særskilt realm/klient, administratorkonto og
produksjonsdatabase. Overstyr OIDC-endepunktene gjennom de dokumenterte
`KEYCLOAK_*`-miljøvariablene; aldri legg produksjonsverdier i
`application.properties`.

Stopp lokale containere uten å slette data:

```bash
docker compose down
```
