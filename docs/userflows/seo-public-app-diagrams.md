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
| 1. Offentlig sitemap og SEO-innganger | Delvis | `/`, `/utstyr`, `/utstyr/{slug}`, sitemap og robots | Kategorisider, `/selg`, `/om`, offentlig innlogging og forespørsel |
| 2. Kjøper finner utstyr fra forsiden | Delvis | Forside, publiserte annonser, katalog, søk og detaljside | Kategorinavigasjon, kjøperkonto, lagring og forespørsel |
| 3. Browse/filter til listedetalj | Delvis | Katalog, fritekst, lokasjon, tilstand, nulltreff og detaljside | Prisfilter og kategori-URL-er |
| 4. Listedetalj til forespørsel eller lagring | Delvis | Publisert detaljside med selger- og dokumentasjonsdata | Forespørsel, lagring, kjøperinnlogging og retur-URL |
| 5. Selger starter fra offentlig side | Delvis | Keycloak-innlogging, `SELLER`-krav på `/app`, privat opprettelse av utkast og publisering | `/selg`, bilder, dokumenter, redigering og arkivering |
| 7. Administrator godkjenner selgersøknad | Planlagt | Beslutningsgrense for roller og bootstrap | Selgersøknad, `/admin`, auditlogg og trygg Keycloak-administrasjonsintegrasjon |

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
    A[Åpner /app] --> B{Har Keycloak-rollen SELLER?}
    B -- Nei --> C[Ingen tilgang til selgerområdet]
    B -- Ja --> D[OIDC-innlogging med Authorization Code og PKCE]
    D --> E[Registrer eller hent SellerAccount fra OIDC subject]
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
    A[Ny identitet registrerer seg hos Keycloak] --> B[E-post verifisert]
    B --> C[Sender selgersøknad med virksomhetsopplysninger]
    C --> D[Lagre søknad som PENDING uten SELLER-rolle]
    D --> E[Markedsplassadministrator åpner /admin]
    E --> F{Har brukeren ADMIN-rolle?}
    F -- Nei --> G[Avvis tilgang]
    F -- Ja --> H[Vurderer virksomhetsopplysninger]
    H --> I{Godkjenn søknad?}
    I -- Nei --> J[Avslå søknad og lagre auditlogg]
    I -- Ja --> K[Tildel SELLER via minst privilegerte Keycloak service-konto]
    K --> L[Opprett eller aktiver SellerAccount]
    L --> M[Lagre beslutning og auditlogg]
    M --> N[Bruker logger inn som SELLER og åpner /app]
```
