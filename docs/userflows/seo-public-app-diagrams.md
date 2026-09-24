# PDF-diagrammer: offentlig SEO-app

Kilde: `Design av digital løsning Fagskolen.pdf`, spesielt sitemapet for offentlig tilgjengelig app:

```text
Hjem → Browse/Utforsk utstyr → Kategori → Listedetaljer
Salg
Om/Kontakt
Logg inn
```

Diagrammene under er også lagret som separate Mermaid-kilder i `docs/userflows/diagrams/` og eksporteres til PDF i `docs/userflows/pdf/`.

## Implementasjonsstatus

Ingen av de fem opprinnelige diagrammene er fullført ende-til-ende. De
beskriver et bredere målbilde enn den implementerte prototypen.

| Diagram | Status | Implementert | Ikke implementert |
| --- | --- | --- | --- |
| 1. Offentlig sitemap og SEO-innganger | Delvis | `/`, `/utstyr`, `/utstyr/{slug}`, `/selg`, sitemap og robots | Kategorisider, `/om`, offentlig innlogging og forespørsel |
| 2. Kjøper finner utstyr fra forsiden | Delvis | Forside, publiserte annonser, katalog, søk og detaljside | Kategorinavigasjon, kjøperkonto, lagring og forespørsel |
| 3. Browse/filter til listedetalj | Delvis | Katalog, fritekst, lokasjon, tilstand, nulltreff og detaljside | Prisfilter og kategori-URL-er |
| 4. Listedetalj til forespørsel eller lagring | Delvis | Publisert detaljside med selger- og dokumentasjonsdata | Forespørsel, lagring, kjøperinnlogging og retur-URL |
| 5. Selger starter fra offentlig side | Delvis | `/selg`, Keycloak-innlogging, `SELLER`-krav på `/app`, privat opprettelse av utkast og publisering | Bilder, dokumenter, redigering og arkivering |
| 7. Administrator godkjenner selgersøknad | Delvis | Server-rendered `/selgersoknad`, vedvarende `PENDING`-søknad, `/app/admin`, auditlogg, aktivering av selgerkonto, administratorutlogging og testet Keycloak Admin API-adapter | E-postverifisert selvregistrering, rate limiting og per-miljø service-konto/hemmelighet |
| 8. Operatør installerer og bootstrapper miljø | Delvis | Lokal Compose, realm-import og beskyttet `/app/admin` | Produksjonsrunbook, hemmelighetsforvaltning og tilgangsregister |
| 9. Lokal demo og rollebytte | Ferdig for lokal demo | Demo-identiteter, Keycloak-profil, søknad, godkjenning, reinnlogging og 403-/innloggingsfeil | Selvregistrering og produksjonsidentiteter |

Diagram 6 dekker den implementerte selgerflyten som ikke fantes da de
opprinnelige diagrammene ble laget.

## 1. Offentlig sitemap og SEO-innganger

```mermaid
flowchart TD
    Search[Søkemotor / delt lenke] --> Home[Hjem /]
    Search --> Browse[Utforsk utstyr /utstyr]
    Search --> Category["Kategori /utstyr/kategori/{kategori}"]
    Search --> Detail["Listedetaljer /utstyr/{slug}"]

    Home --> HomeSearch[Søk etter utstyr]
    Home --> FeaturedCategories[Utvalgte kategorier]
    Home --> FeaturedListings[Utvalgte annonser]

    HomeSearch --> Browse
    FeaturedCategories --> Category
    FeaturedListings --> Detail
    Browse --> Category
    Category --> Detail

    Home --> Sell[Selg utstyr /selg]
    Home --> About[Om/Kontakt /om]
    Home --> Login[Logg inn /app/login]

    Sell --> Login
    Detail --> Inquiry[Send forespørsel]
    Inquiry --> Login
```

## 2. Kjøper finner utstyr fra forsiden

```mermaid
flowchart TD
    A[Åpner forsiden] --> B{Har konkret behov?}
    B -- Ja --> C[Skriver søkeord]
    B -- Nei --> D[Velger utvalgt kategori]
    C --> E[Server-rendered søkeresultat]
    D --> F[Server-rendered kategoriside]
    E --> G[Skanner produktkort]
    F --> G
    G --> H{Relevant annonse?}
    H -- Nei --> I[Justerer søk eller kategori]
    I --> E
    H -- Ja --> J[Åpner listedetaljer]
    J --> K[Vurderer pris, lokasjon, tilstand og dokumentasjon]
    K --> L{Neste steg}
    L -- Send forespørsel --> M[Logg inn eller opprett kjøperkonto]
    L -- Lagre annonse --> M
    L -- Del/skriv ut --> N[Bruker offentlig side uten innlogging]
```

## 3. Browse/filter til listedetalj

```mermaid
flowchart TD
    A[Åpner /utstyr] --> B[Ser server-rendered katalog]
    B --> C[Setter filter]
    C --> D{Filtertype}
    D -- Lokasjon --> E["/utstyr?lokasjon=..."]
    D -- Tilstand --> F["/utstyr?tilstand=..."]
    D -- Pris --> G["/utstyr?pris=..."]
    D -- Kategori --> H["/utstyr/kategori/{kategori}"]
    E --> I[Resultatliste]
    F --> I
    G --> I
    H --> I
    I --> J{Treff?}
    J -- Nei --> K[Nulltreff med forslag om bredere filter]
    K --> B
    J -- Ja --> L[Velger annonse]
    L --> M[Listedetalj med dokumentasjon og selgerinfo]
```

## 4. Listedetalj til forespørsel eller lagring

```mermaid
flowchart TD
    A[Åpner listedetalj] --> B[Ser bilder og produktinformasjon]
    B --> C[Kontrollerer dokumentasjon og sertifikater]
    C --> D[Leser informasjon om selger]
    D --> E{Tillitsnivå høyt nok?}
    E -- Nei --> F[Forlater siden eller går tilbake til katalog]
    E -- Trenger mer info --> G[Send forespørsel]
    E -- Ja, men senere --> H[Lagre annonse]
    G --> I{Innlogget?}
    H --> I
    I -- Nei --> J[Logg inn / opprett kjøperkonto med retur-URL]
    I -- Ja --> K[Opprett forespørsel eller lagring i appen]
    J --> K
    K --> L[Bruker returneres til annonse/appstatus]
```

## 5. Selger starter fra offentlig side

```mermaid
flowchart TD
    A[Åpner Hjem eller Selg utstyr] --> B[Leser hva markedsplassen tilbyr]
    B --> C{Vil selge utstyr?}
    C -- Nei --> D[Om/Kontakt eller tilbake til katalog]
    C -- Ja --> E[Klikker Selg utstyr]
    E --> F{Innlogget som selger?}
    F -- Nei --> G[Logg inn / opprett selgerkonto]
    F -- Ja --> H[Vaadin Flow: Opprett annonse]
    G --> H
    H --> I[Last opp bilder]
    I --> J[Legg inn produktdetaljer]
    J --> K[Pris og dokumentasjon]
    K --> L{Fullfør nå?}
    L -- Ja --> M[Send til verifisering/publisering]
    L -- Nei --> N[Lagre som utkast]
```

## 6. Innlogget selger oppretter privat utkast

```mermaid
flowchart TD
    A[Åpner /app] --> B[OIDC-innlogging med Authorization Code og PKCE]
    B --> C{Har Keycloak-rollen SELLER?}
    C -- Nei --> D[Vis 403-side med tilgangsveiledning]
    C -- Ja --> E[Registrer eller hent SellerAccount fra OIDC subject]
    E --> F[Vis egne utkast]
    F --> G[Fyll ut slug, tittel, lokasjon, tilstand, kategori, pris og beskrivelse]
    G --> H{Gyldig og unik slug?}
    H -- Nei --> I[Vis valideringsfeil]
    I --> G
    H -- Ja --> J[Lagre som DRAFT med SellerAccount som eier]
    J --> K[Vis utkast bare for samme SellerAccount]
    K --> L[Velger Publiser]
    L --> M{Eier fortsatt utkastet?}
    M -- Nei --> N[Avvis publisering]
    M -- Ja --> O[Sett PUBLISHED og publiseringsdato]
    O --> P[Vises i offentlig katalog, detaljside og sitemap]
```

## 7. Administrator godkjenner selgersøknad

```mermaid
flowchart TD
    A[Autentisert OIDC-identitet uten SELLER] --> C[Sender selgersøknad på /selgersoknad]
    C --> D[Lagre søknad som PENDING uten SELLER-rolle]
    D --> E[Markedsplassadministrator åpner /app/admin]
    E --> F{Har brukeren ADMIN-rolle?}
    F -- Nei --> G[Avvis tilgang]
    F -- Ja --> H[Vurderer virksomhetsopplysninger]
    H --> I{Godkjenn søknad?}
    I -- Nei --> J[Avslå søknad og lagre auditlogg]
    I -- Ja --> K[Tildel SELLER via minst privilegerte Keycloak service-konto]
    K --> L{Rolletildeling bekreftet?}
    L -- Nei --> M[Behold PENDING og vis eksplisitt feil]
    L -- Ja --> N[Opprett eller aktiver SellerAccount]
    N --> O[Lagre beslutning og auditlogg]
    J --> Q{Flere ventende søknader?}
    O --> Q
    M --> Q
    Q -- Ja --> H
    Q -- Nei --> R[Administrator logger ut med knappen «Logg ut» i /app/admin]
    O --> P[Bruker logger inn på nytt som SELLER og åpner /app]
```

## 8. Operatør installerer og bootstrapper miljø

```mermaid
flowchart TD
    A[Operatør starter nytt miljø] --> B[Opprett separate PostgreSQL-databaser for markedsplass og Keycloak]
    B --> C[Hent hemmeligheter fra miljøets hemmelighetsforvaltning]
    C --> D[Deploy Keycloak med HTTPS, produksjonsdatabase og dedikert realm]
    D --> E[Opprett eller roter Keycloak-plattformadministrator]
    E --> F[Opprett første navngitte markedsplassadministrator]
    F --> G[Tildel kun realmrollen ADMIN]
    G --> H[Dokumenter tilgang i miljøets tilgangsregister]
    H --> I[Deploy markedsplass med Flyway-migreringer]
    I --> J[Verifiser offentlig katalog, OIDC-login og /app/admin]
    J --> K{ADMIN har tilgang og SELLER mangler /app/admin?}
    K -- Nei --> L[Stopp utrulling og korriger autorisasjon]
    K -- Ja --> M[Miljø klart for selgersøknader]
```

## 9. Lokal demo og rollebytte

```mermaid
flowchart TD
    A[Start docker compose og app med lokal .env] --> B[Åpne /selg]
    B --> C{Hvilken demoidentitet?}
    C -- seller-demo --> D[Åpne /app]
    D --> E[Logg inn med SELLER]
    E --> F[Opprett og publiser eget utkast]
    C -- buyer-demo --> G[Åpne /selgersoknad]
    G --> H[Fullfør Keycloak-profil hvis påkrevd]
    H --> I[Send søknad og se PENDING]
    I --> J[Logg ut]
    J --> K[Logg inn som admin-demo på /app/admin]
    K --> L[Godkjenn søknaden]
    L --> M{Keycloak tildeler SELLER?}
    M -- Nei --> N[Vis driftsmelding og behold PENDING]
    M -- Ja --> O[Auditlogg lagres]
    O --> P[Logg ut og inn igjen som buyer-demo]
    P --> Q[Åpne /app som SELLER]
    C -- seller-demo på /app/admin --> R[Vis 403-side]
    H -- Innlogging avbrutt --> S[Vis innloggingsfeil med nytt forsøk]
```
