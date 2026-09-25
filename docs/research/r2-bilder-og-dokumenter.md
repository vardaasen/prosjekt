# R2: Bilder og dokumenter på annonser

Forskningsnotat for issue vardaasen/prosjekt#14. Kilder hentet 2026-09-25. Alle påstander har kildehenvisning; der noe ikke kunne bekreftes i primærkilde, står det eksplisitt **(ikke bekreftet)**.

## Kort oppsummering

- **Tillat bare JPEG og PNG for bilder og PDF for dokumenter.** Dette er formatene Java leser uten ekstra avhengigheter (ImageIO for bilder, PDFBox for PDF). HEIC og WebP krever egne biblioteker, og HEIC krever native kode med LGPL-/patentspørsmål.
- **Avgjør filtypen ut fra innholdet, ikke fra filnavnet.** Apache Tika (`tika-core`, Apache 2.0) sjekker magiske bytes. Grensene settes på serveren: i Vaadin `UploadHandler` og Spring multipart. Vaadins standard er *ingen* grense for filstørrelse.
- **Alle bilder kodes om på serveren.** Bildet dekodes, EXIF-rotasjonen brukes, bildet skaleres ned og skrives som ny JPEG. Da forsvinner EXIF og GPS, og polyglot-innhold ødelegges. OWASP anbefaler slik omskriving eksplisitt.
- **PDF behandles som upålitelig også etter nedlasting.** Krypterte PDF-er og PDF-er over sidegrensen avvises. Alle aktive konstruksjoner fjernes før lagring: dokument-JavaScript, OpenAction og tilleggshandlinger (AA) på katalog, sider og annotasjoner, handlinger på lenker og skjemafelt, AcroForm/XFA og innebygde filer. Lykkes ikke fjerningen, avvises filen. Metadata fjernes med PDFBox. Svarhodene `attachment`, `nosniff` og CSP `sandbox` beholdes, men de beskytter bare nettleseren, ikke en PDF-leser som åpner filen lokalt.
- **Virusskanning med ClamAV (GPLv2) er mulig som egen container via clamd `INSTREAM`**, men trenger 3–4 GB RAM. Det foreslås som en egen compose-profil, og produkteier må ta stilling til det.
- **Lagring: PostgreSQL `bytea` i egen tabell bak et `FileStorage`-grensesnitt.** Det er enklest for prototypen: én backup, sletting i samme transaksjon og ingen ekstra tjeneste. MinIO er arkivert og distribueres bare som kildekode. SeaweedFS (Apache 2.0) er et aktuelt S3-alternativ senere. Garage (AGPLv3) er også et alternativ, men mangler blant annet bucket policies.
- **LLM/visjonsmodell tas ikke med i første versjon.** Enkle kontroller med biblioteker pluss admin-godkjenning dekker behovet. En LLM ville sendt bildepiksler og dokumentinnhold, som kan inneholde navn og signaturer, til en tredjepart.
- **Bokføringsloven pålegger ikke plattformen å oppbevare selgerens vedlegg.** Plattformen er ikke part i handelen. Vedleggene kan slettes kort tid etter at annonsen avsluttes. Reglene for digital plattforminformasjon (DPI) i Norge omfatter per forskriften ikke salg av varer.

---

## 1. Formater

### Bilder

| Format | Java-støtte | Nettleserstøtte | Vurdering |
| --- | --- | --- | --- |
| JPEG | Standard i `javax.imageio` (les/skriv) [1] | Alle [4] | **Tillat.** Standardformat for mobilfoto. |
| PNG | Standard i `javax.imageio` (les/skriv) [1] | Alle [4] | **Tillat.** Skjermbilder og skannede sider. |
| WebP | Ikke i JDK [1]. TwelveMonkeys (BSD-3) kan bare *lese* WebP [2] | Alle moderne [4] | Kan tas med senere. Det gir en ekstra avhengighet og lite gevinst fordi vi uansett koder om til JPEG. |
| HEIC/HEIF | Ikke i JDK [1] eller TwelveMonkeys [2]. libheif er LGPL (C/C++) uten offisiell Java-binding, og HEIC-koding bruker x265, som er GPL [3] | MDN nevner ikke HEIC blant web-formater [4] | **Ikke tillat.** Krever native kode og lisens-/patentvurdering. |
| GIF/BMP/TIFF | Standard i JDK [1] | Varierer | Ikke nødvendig. Holdes utenfor for å minimere angrepsflaten. |

**Om iPhone-bilder (HEIC):** Sekundærkilder oppgir at iOS konverterer HEIC til JPEG når nettsiden bare aksepterer `image/jpeg` via `<input type="file" accept=…>`, og at Safari 17+ oppfører seg annerledes når `image/heic` står i `accept` [5]. **(ikke bekreftet i Apple/WebKit-primærkilde)** Dette må testes på en ekte iPhone. Anbefalingen er å *ikke* ta med `image/heic` i `accept` og å gi en tydelig feilmelding dersom en HEIC-fil likevel kommer inn.

### Dokumenter i bransjen

Servicehistorikk, sertifikater, manualer, inspeksjonsrapporter og CE-/samsvarsdokumentasjon kommer i praksis i tre former:

1. **Born-digital PDF** fra leverandør eller klasseselskap. Den er søkbar og kan inneholde skjemaer, lenker, metadata og i verste fall JavaScript.
2. **Skannet PDF** fra kontorskanner. Hver side er et bilde, uten tekstlag.
3. **Mobilfoto av papirdokument.** Det er i praksis et JPEG-bilde. I stedet for å godta dette som eget format bør brukeren oppfordres til å bruke skannefunksjonen på telefonen, som lager PDF, eller laste opp bildet som JPEG i dokumentfeltet (se åpne spørsmål).

Det finnes ikke en primærkilde som beskriver hvor vanlige disse formene er i akvakulturbransjen. Fordelingen over bygger på domenekunnskap og er **(ikke bekreftet)**.

Office-formater (DOCX/XLSX) og ZIP bør ikke tillates. OWASP fraråder ZIP fordi slike filer «kan inneholde alle typer filer», og peker på zip- og XML-bomber [6].

### Anbefalt minimal allow-list

| Kategori | MIME (etter innholdsdeteksjon) | Endelser (bare UX) |
| --- | --- | --- |
| Bilde | `image/jpeg`, `image/png` | `.jpg`, `.jpeg`, `.png` |
| Dokument | `application/pdf` | `.pdf` |

---

## 2. Sikkerhet ved opplasting

### Grunnlinje fra OWASP File Upload Cheat Sheet [6]

- Bruk en allow-list over endelser. Stol ikke på `Content-Type`, men valider filsignaturen.
- Generer tilfeldige filnavn på serveren (UUID). Lagre utenfor webroot eller på separat lagring, og bruk minste privilegium.
- Sett størrelsesgrenser, også etter eventuell dekomprimering.
- Krev autentisering og autorisasjon for opplasting, beskytt mot CSRF og rate-limit.
- Kjør filer gjennom antivirus/sandbox eller CDR. OWASP skriver at *«image rewriting techniques destroys any kind of malicious content injected in an image»*. For dokumenter nevnes CDR for PDF.

Prosjektets egen `docs/security-baseline.md` krever allerede allow-list for MIME/type, størrelsesgrense, viruskontroll, tilfeldig lagringsnavn og lagring som ikke kan serveres direkte.

### Innholdsdeteksjon uavhengig av endelse: Apache Tika

- Tika finner typen fra magiske bytes med `MagicDetector`, fra navn med `NameDetector` (raskt, men upålitelig) og for container-formater. For magi- og navnedeteksjon holder `tika-core` [7].
- Lisensen er Apache 2.0 [8]. Tika 4.0.0 kom 2026-08-21 og krever Java 17+. 3.x-linjen (3.3.2, 2026-07-16) vedlikeholdes fortsatt [9].
- Tikas sikkerhetsside lister mange OOM- og uendelig-løkke-sårbarheter i *parserne* [10]. **Bruk bare deteksjon (`tika-core`), ikke `tika-parsers`.** At `tika-core` alene har lavere risiko, står ikke eksplisitt i Tikas dokumentasjon **(ikke bekreftet)**. Det følger av at parserne er der sårbarhetene er rapportert.
- Et enklere alternativ er å sjekke de få signaturene selv (`FF D8 FF` for JPEG, `89 50 4E 47` for PNG, `%PDF-` for PDF). Prinsippet om rammeverk fremfor egenutviklet kode taler for Tika.

### Størrelsesgrenser

- Vaadin `UploadHandler`: standard `getFileSizeMax()` og `getRequestSizeMax()` er **-1 (ingen grense)**, og `getFileCountMax()` er 10 000 [11]. Grensene må derfor overstyres.
- Spring Boot `spring.servlet.multipart.max-file-size` har standard `1MB`, og `max-request-size` har `10MB` [12]. Vaadin sier at Spring håndhever disse også for Vaadin-opplasting, og at begge må konfigureres [13].
- `upload.setMaxFileSize()` og `setAcceptedFileTypes()` på klienten er bare UX. *«client-side restrictions are UI-only»* [13].

### Skadevareskanning: ClamAV

- Lisensen er GPLv2 [14]. Offisielt Docker-image er `clamav/clamav`, og clamd lytter på TCP 3310 [15].
- **RAM:** minimum 3 GB og helst 4 GB. Signaturene alene bruker over 1,2 GiB, og ved oppdatering av signaturene trengs det midlertidig dobbelt så mye [15].
- **Integrasjon:** appen strømmer bytes til clamd med `INSTREAM` (chunks på formen `<4-byte lengde><data>`, avsluttet med en chunk på lengde 0). Grensen er `StreamMaxLength`, med standard 100M [16][17].
- clamd **autentiserer ikke TCP-trafikk** og tar imot kommandoer fra alle som når socketen [18]. Den må derfor bare ligge på compose-nettverket og ikke publiseres med `ports:`.
- `MaxFileSize` er 100M som standard. Større filer skannes ikke og regnes som rene, med mindre `AlertExceedsMax` er slått på. `ScanPDF` er på som standard, og `AlertEncrypted` er av [17].
- Lisens i praksis: appen snakker med clamd over en nettverkssocket og lenker ikke GPL-kode. Om dette unngår GPL-avledning står ikke i ClamAVs dokumentasjon **(ikke bekreftet, ikke juridisk vurdert)**.

### Risiko i PDF

- En PDF kan ha dokument-JavaScript som kjøres ved åpning, en OpenAction, tilleggshandlinger (AA), AcroForm-skjemaer og innebygde filer. PDFBox eksponerer alt dette: `PDDocumentNameDictionary.getJavaScript()`, som PDFBox beskriver som *«When the document is opened, all the JavaScript actions in it shall be executed»*, `getEmbeddedFiles()`, og `PDDocumentCatalog.getOpenAction()`, `getActions()` og `getAcroForm()` [19][20].
- **Tiltak som kan gjennomføres:** avvis krypterte PDF-er (`PDDocument.isEncrypted()` [21]) og PDF-er med dokument-JavaScript, OpenAction av JavaScript- eller Launch-type eller innebygde filer. Fjern metadata, og lagre en ny kopi med `PDDocument.save()`.
- **Sjekkene over er ikke nok alene.** Handlinger på sider (`PDPage`-tilleggshandlinger), på annotasjoner og lenker, og i skjemafelt dekkes ikke av dem. Svarhodene `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff` og CSP `sandbox` [23] hindrer skript i nettleseren, men de endrer ikke filen. En administrator eller kjøper som laster ned og åpner PDF-en i en lokal eller ekstern PDF-leser er fortsatt eksponert for handlinger som ligger igjen i filen (Aikido-funn på PR #31).
- **Anbefalt tiltak:** gå gjennom katalog, alle sider og alle annotasjoner med PDFBox og fjern alle handlinger (JavaScript, Launch, SubmitForm, ImportData, GoToR/GoToE og tilleggshandlinger), AcroForm og XFA, og innebygde filer. Lagre så en ny kopi. Hvis fjerningen feiler eller filen ikke kan tolkes, avvises den. At PDFBox-APIet dekker alle disse stedene, må verifiseres med testfiler **(ikke bekreftet)**.
- **Høyeste sikkerhetsnivå** er full CDR: rastrer hver side til bilde med PDFBox [22]. Da forsvinner all aktiv kode, men dokumentet mister tekst og søkbarhet, og filen blir større. Det er et alternativ hvis originalen ikke skal beholdes, eller som en ekstra forhåndsvisning ved siden av originalen.
- Hvis originalen beholdes for nedlasting, skal brukeren få en tydelig advarsel om at filen kommer fra en annen virksomhet.
- PDFBox 3 bruker en cache bare i minnet som standard. Med `IOUtils`/`ScratchFile` kan cachen legges på midlertidig fil [24]. For store eller ondsinnede PDF-er gir det mindre minnepress. Ved OOM foreslår PDFBox scratch-fil [25].
- PDFBox har lisens Apache 2.0. Nåværende versjon er 3.0.8 (2026-07-11) [22].

### Risiko i bilder

- **Polyglotter** er filer som er gyldige som både bilde og for eksempel HTML/JS eller PDF. De nøytraliseres når vi dekoder og skriver et nytt bilde fra piksler. Det er OWASPs omskrivningstiltak [6].
- **Dekompresjonsbomber (pixel flood)** er små filer med enorme dimensjoner. Les bredde og høyde med `ImageReader.getWidth(0)`/`getHeight(0)` før `read()`, og avvis over en pikselgrense (for eksempel 50 MP). Javadoc sier ikke eksplisitt at disse kallene unngår full dekoding [26] **(ikke bekreftet at ingen dekoding skjer, må verifiseres med test)**. `ImageReadParam.setSourceSubsampling` kan brukes til å lese nedskalert [26].
- Apache Commons Imaging (Apache 2.0, fortsatt `1.0.0-alpha6`) garanterer at ondsinnede bilder ikke gir kodekjøring, men *ikke* fravær av DoS [27][28]. Det er et argument for å holde oss til JDK ImageIO pluss pikselgrense og ikke legge til flere dekodere.

---

## 3. Personvern: metadata og innhold

### Personopplysninger i filer

- Personopplysninger er alle opplysninger som kan knyttes til en person, direkte eller indirekte. Datatilsynet nevner blant annet gjenkjennelige bilder og indirekte identifikatorer [29].
- **Foto:** EXIF kan inneholde GPS-posisjon, tidspunkt, kameramodell, serienummer og eiernavn. EXIF, IPTC og XMP kan leses med metadata-extractor (Apache 2.0, bare lesing) [30]. Posisjonen kan avsløre hvor et anlegg eller en person befinner seg. Selve bildet kan vise personer, båtnavn eller registreringsnummer.
- **PDF:** Info-ordboken (Author, Creator, Producer) og XMP-metadata. `PDDocument.getDocumentInformation()` og `PDDocumentCatalog.getMetadata()` gir tilgang til begge og kan settes til tom eller `null` [20][21]. Innholdet kan inneholde navn, signaturer, adresser og ansattnavn på servicerapporter.

### Stripping i Java

| Metode | Lisens | Merknad |
| --- | --- | --- |
| **Omkoding med ImageIO** (anbefalt) | JDK | Ved JPEG-skriving uten metadata skrives bare JFIF `APP0`. EXIF (`APP1`) behandles som «unknown» marker og følger ikke med når vi skriver et nytt `BufferedImage` [31]. Fjerner også polyglot-innhold. |
| metadata-extractor | Apache 2.0 [30] | Trengs bare for å lese EXIF Orientation *før* omkoding. Om ImageIO selv tar hensyn til orientering står ikke i JDK-dokumentasjonen **(ikke bekreftet)**; må testes med rotert mobilfoto. |
| Commons Imaging `ExifRewriter` | Apache 2.0 | Tapsfri fjerning uten omkoding. Biblioteket er alfa og gir ingen DoS-garanti [27][28]. Ikke nødvendig når vi koder om. |
| PDFBox | Apache 2.0 [22] | Fjern Info-ordbok og XMP, lagre ny fil. |

**Løser omkoding metadataproblemet?** For bilder, ja, når vi skriver et nytt `BufferedImage` uten å kopiere `IIOMetadata`. Synlig innhold som personer, navn og skilt i bildet fjernes ikke. For PDF fjerner omkoding bare metadata, ikke navn og signaturer i selve dokumentet.

### Konsekvenser for oppbevaring og sletting

- GDPR art. 5 nr. 1 bokstav c (dataminimering) og bokstav e (lagringsbegrensning), art. 17 (sletting) og art. 25 (innebygd personvern og personvern som standard) gjelder som norsk lov gjennom personopplysningsloven § 1 [32][33]. Datatilsynet beskriver innebygd personvern som at personvern skal «tas hensyn til i alle utviklingsfaser» [34].
- Konsekvenser for oss:
  - Selgeren må få beskjed før opplasting: «Sladd navn/signaturer som ikke er nødvendige.»
  - Dokumenter er lukket som standard (se punkt 5).
  - Vedlegg slettes når annonsen slettes, avvises eller avsluttes, etter en kort frist.
  - Revisjonsloggen lagrer bare ID, hash og beslutning, aldri innhold.
  - Backup-rotasjonen setter en øvre grense for når slettede data er borte også fra backup.

---

## 4. Validering av innhold

### Hva biblioteker kan sjekke automatisk (anbefalt først)

| Kontroll | Hvordan | Kilde |
| --- | --- | --- |
| Filtype | Tika `tika-core` | [7] |
| Integritet (bilde) | `ImageIO.read` må lykkes | [1] |
| Oppløsning, min/maks | `ImageReader.getWidth/getHeight` | [26] |
| Orientering | EXIF Orientation via metadata-extractor, rotér før omkoding | [30] |
| Integritet (PDF) | `Loader.loadPDF` må lykkes | [24] |
| Sidetall | `PDDocument.getNumberOfPages()` | [21] |
| Kryptert/aktivt innhold | `isEncrypted()`, JavaScript, OpenAction, EmbeddedFiles | [19][20][21] |
| Skannet eller søkbar PDF | Tekstuttrekk med PDFBox; tom tekst tyder på skann | [22] |
| Uskarphet | Laplace-varians krever OpenCV (Apache 2.0, native bibliotek) | [35] |

Uskarphetssjekk krever et nytt native bibliotek og er ikke «enkelt». **Utsett den.** Admin ser uansett bildene.

### Hva bare en LLM/visjonsmodell kan vurdere

- Om bildet faktisk viser utstyret som beskrives, om dokumentet er lesbart og riktig type (for eksempel servicerapport eller sertifikat), og forslag til alt-tekst.
- **Data som forlater plattformen:** bildepiksler og PDF-innhold, altså navn, signaturer, adresser, personer i bilder og eventuelt posisjon via landemerker. EXIF sendes ikke hvis vi sender det omkodede bildet. Claude leser for øvrig ikke bildemetadata [36].
- **Hvor data behandles:**
  - Mistral: EU som standard, med mulig midlertidig overføring til underleverandører etter art. 46 [37].
  - OpenAI: EU-dataresidens krever godkjenning og nytt prosjekt. Regional lagring innebærer ikke nødvendigvis regional behandling [38].
  - Anthropics førstepartis API: `inference_geo` er `global` eller `us`. Data lagres i USA. EU-region er tilgjengelig via Google Cloud [39].
- **Kostnad:** Anthropic oppgir som eksempel ca. 1,30 USD per 1000 bilder på 1 MP med Claude Haiku 4.5 [36]. Kostnaden er lav. Den større utgiften er personvernet: databehandleravtale (art. 28), overføringsvurdering, beslutningsnotat etter 0009 og at selger må informeres.

### Anbefaling

1. **Nå:** bare kontrollene med biblioteker i tabellen over. De avviser åpenbart feil tidlig i opplastingsdialogen, slik at admin ikke oversvømmes.
2. **Nå:** påkrevd alt-tekst per bilde (WCAG 2.2 SC 1.1.1: tekstalternativ med samme formål; dekorative bilder får `alt=""`) [40]. Admin godkjenner alt-teksten sammen med annonsen.
3. **Senere, eget beslutningsnotat:** LLM-forhåndsvurdering bare av bilder, der modellen gir forslag til admin og ikke avgjør selv. En EU-leverandør foretrekkes. Dokumenter sendes ikke.

---

## 5. Lagring

### Alternativer

| | PostgreSQL `bytea` | PostgreSQL Large Objects | S3-kompatibel objektlagring | Filsystem-volum |
| --- | --- | --- | --- | --- |
| Maks størrelse | 1 GB per verdi (TOAST) [41] | 4 TB [42] | Praktisk talt ubegrenset | Diskstørrelse |
| Strømming | Hele verdien leses eller skrives samlet [42] | Delvis lesing/skriving [42] | Ja (HTTP range) | Ja |
| Backup | Med i `pg_dump`/PITR | Med i dump, men egen håndtering | Egen backup | Egen backup |
| Sletting (GDPR) | `DELETE` i samme transaksjon som metadata | `lo_unlink` + risiko for foreldreløse objekter, `vacuumlo` [43] | To systemer: faren er ikke-transaksjonell sletting og foreldreløse objekter | Samme problem som S3 |
| Tilgangskontroll | Bare via appen | Egne GRANT per LO [44] | Presignerte URL-er eller appen. **Aldri offentlig bucket** | Bare via appen |
| Ekstra tjeneste | Nei | Nei | Ja | Nei (volum) |

### Status for S3-alternativer

- **MinIO:** AGPLv3. GitHub-repoet ble arkivert 2026-04-25. Community-utgaven *«is now distributed as source code only»*, og ferdigbygde binærfiler vedlikeholdes ikke lenger [45]. **Ikke egnet.**
- **SeaweedFS:** Apache 2.0, aktivt, med bred S3-støtte (versjonering, Object Lock, lifecycle, bucket policies). Det finnes en kommersiell Enterprise-utgave med ekstra funksjoner [46].
- **Garage:** AGPLv3 [47][48], EU-finansiert via NGI/NLnet og laget for små selv-hostede installasjoner [49]. Mangler bucket policies/ACL, versjonering og Object Lock, men har presignerte URL-er og lifecycle-`Expiration` [50]. AGPL gir bare plikter ved *endret* versjon som tilbys over nett, men krever likevel lisensvurdering etter 0009.

### Offentlige bilder og private dokumenter

- **Bilder på publiserte annonser** må kunne crawles på SEO-sidene. Server dem via en Spring MVC-rute, for eksempel `/annonser/{slug}/bilder/{id}.jpg`, som sjekker at annonsen er `PUBLISHED` og at filen er et bilde. Bruk `Cache-Control: public` og ETag fra SHA-256. Da blir tilgangskontrollen én server-side regel og ikke bucket-policy.
- **Dokumenter** er lukket som standard. Server dem bare via en autorisert rute med `attachment`, `nosniff` og CSP `sandbox` [23]. Om kjøpere må være innlogget for å laste ned, er et spørsmål for produkteier.
- Vaadin-flaten kan vise forhåndsvisning via `DownloadHandler.fromInputStream`. Der er `VaadinSession` tilgjengelig for autorisasjonssjekk [51].

### Anbefaling

**PostgreSQL `bytea`** i en egen tabell (`listing_file_content`) adskilt fra metadatatabellen, bak et `FileStorage`-grensesnitt i domenet.

- Omkodede bilder blir små. Maks 2048 px langside gir anslagsvis 0,3–1 MB **(anslag, ikke målt)**. PDF-er begrenses til 20 MB. Dette er langt under grensen på 1 GB.
- Kolonnen settes til `STORAGE EXTERNAL`, fordi JPEG og PDF allerede er komprimert. EXTERNAL gir lagring utenfor raden uten ny komprimering [41].
- Én database gir én backup, én Flyway-migrering, transaksjonell sletting og ingen ny tjeneste i Compose.
- Unngå Large Objects. Foreldreløse objekter og egne rettigheter gir mer kompleksitet uten gevinst ved disse størrelsene [43][44].
- Bytt til SeaweedFS bak samme grensesnitt hvis volum eller trafikk krever det.

---

## 6. Vaadin Flow 25 Upload

- Upload er Apache 2.0 (`vaadin-upload-flow-parent/LICENSE`) [52], altså ikke kommersiell. Modulær opplasting (`UploadManager`, `UploadButton`, `UploadDropZone`, `UploadFileList`) finnes [53]. Lisensen for den er ikke oppgitt på dokumentasjonssiden **(ikke bekreftet)**.
- Innebygde handlere [11][13][54]:
  - `UploadHandler.inMemory(...)` samler hele filen i `ByteArrayOutputStream` og setter selv ingen grense. *«each concurrent upload holds the entire file in heap memory»* [13][55].
  - `UploadHandler.toTempFile(...)` strømmer til midlertidig fil og holder minnebruken lav. **Appen må selv slette filen** (`try/finally`). `File.deleteOnExit()` skal ikke brukes. Vaadin anbefaler også periodisk opprydding av filer eldre enn 24 timer [13].
  - `UploadHandler.toFile(...)` skriver til en katalog du velger.
- Validering i tre faser: metadata (navn og deklarert type), header (`validateHeader(byteCount, callback)` for magiske bytes) og complete. `UploadEvent.reject()` i metadata- eller header-fasen stopper opplastingen tidlig [54].
- Grenser: overstyr `getFileSizeMax()`, `getRequestSizeMax()` og `getFileCountMax()` i en egen handler, der standard er -1/-1/10 000 [11]. Sett også `spring.servlet.multipart.*` [12][13].
- Opplastingen tas imot uavhengig av komponentens livssyklus. UI-et holdes tilknyttet sesjonen så lenge opplastingen pågår [56].

**Slik passer det sammen:** bruk `toTempFile`. Sjekk magiske bytes med Tika i header-fasen. Kjør resten av pipelinen (skanning, omkoding og lagring) på temp-filen i complete-fasen, og slett temp-filen i `finally`. Tyngre behandling kan kjøres i bakgrunnsjobb, slik Vaadin anbefaler [13].

---

## 7. Oppbevaring etter norsk rett

- **Bokføringsloven** gjelder den som er bokføringspliktig (§ 2) og *dens egen* regnskapsdokumentasjon [57]. § 13 skiller mellom to frister. Nr. 1–4 skal oppbevares i 5 år: årsregnskap, spesifikasjoner, dokumentasjon av bokførte opplysninger og revisors kommunikasjon. Nr. 5–8 skal oppbevares i 3 år og 6 måneder: avtaler, vesentlig korrespondanse, pakksedler og prisoversikter [58]. Elektronisk regnskapsmateriale kan ligge i et annet EØS-land, Storbritannia eller Sveits etter bokføringsforskriften § 7-5 [59].
  - **Konsekvens:** plattformen er ikke part i handelen og utsteder ikke salgsdokumentet. Servicehistorikk og sertifikater på en annonse er ikke plattformens regnskapsmateriale. **Bokføringsloven pålegger derfor ikke plattformen å oppbevare vedleggene.** Plattformens *egne* bilag, som faktura for eventuelle gebyrer, omfattes.
  - Selger og kjøper har selv plikten for sine egne bilag. Dette er en tolkning av lovteksten og ikke juridisk rådgivning **(ikke vurdert av jurist)**.
- **Kjøpsloven § 32:** den absolutte reklamasjonsfristen i næringskjøp er 2 år etter overtakelse, med mindre det er gitt garanti [60]. Det kan være et argument for at *partene* tar vare på dokumentasjonen. Det gir ikke plattformen noen plikt.
- **Digital plattforminformasjon (DPI):** skatteforvaltningsloven § 7-11 gjelder fra 2026-01-01 [61][62]. Forskriften § 7-11-3 omfatter utleie av fast eiendom og transportmidler og salg av tjenester, men ikke salg av varer [63]. Oppbevaringsplikten på 5 år gjelder identifikasjonsopplysninger om selgere, ikke annonsevedlegg [63]. Skatteetaten har en høring om *«opplysningsplikt om salgs- og kjøpstransaksjoner»* [64] som ikke ble undersøkt nærmere **(ikke bekreftet om den treffer varesalg på B2B-plattformer)**.
- **GDPR art. 17 nr. 3 bokstav b** gir unntak fra sletteretten når en rettslig forpliktelse krever lagring [32]. Vi har ikke funnet en slik forpliktelse for vedleggene.

---

## Anbefalt minimal design

### Allow-list og grenser (forslag, produkteier justerer)

| | Bilder | Dokumenter |
| --- | --- | --- |
| Innhold (Tika) | `image/jpeg`, `image/png` | `application/pdf` |
| Maks filstørrelse | 15 MB | 20 MB |
| Andre grenser | ≤ 50 MP, langside ≥ 800 px | ≤ 100 sider, ikke kryptert, uten JS/OpenAction/innebygde filer |
| Antall per annonse | 12 | 10 |
| Lagret form | Ny JPEG, langside ≤ 2048 px, uten metadata | Ny PDF-kopi uten Info/XMP |
| Påkrevd felt | Alt-tekst (WCAG 1.1.1) | Dokumenttype (servicehistorikk/sertifikat/manual/inspeksjon/CE) |

### Pipeline

```
Selger (innlogget, SELLER, eier utkastet) i /app
 └─ Vaadin Upload (accept + maxFileSize + maxFiles = bare UX)
     └─ UploadHandler.toTempFile, egne grenser + spring.servlet.multipart.*
         ├─ metadata-fase: antall og størrelse, eierskap til utkast
         ├─ header-fase:   Tika-deteksjon på første bytes → avvis utenfor allow-list
         └─ complete-fase (try/finally sletter temp-fil):
             1. Tika på hele filen (må samsvare med header-fasen)
             2. [valgfritt, compose-profil] ClamAV INSTREAM → funn/feil = avvis (fail-closed)
             3a. Bilde: dimensjoner før dekoding → dekod → EXIF-orientering → skaler → ny JPEG
             3b. PDF:  PDFBox (temp-fil-cache) → ikke kryptert, sider ≤ grense
                       → fjern alle handlinger (katalog, sider, annotasjoner, lenker,
                         skjemafelt), AcroForm/XFA og innebygde filer
                       → fjern Info/XMP → lagre ny kopi; feiler noe → avvis
             4. SHA-256, UUID, lagre metadata + bytes i én transaksjon
             5. Vis resultat eller feilmelding i dialogen (tidlig tilbakemelding til selger)
 └─ Annonse sendes til godkjenning → admin ser bilder, alt-tekst og dokumenter
 └─ Publisert: bilder via SEO-rute (bare PUBLISHED), dokumenter via autorisert nedlasting
 └─ Slettet/avvist/avsluttet + frist: DELETE av metadata og innhold; backup roterer ut
```

Opprinnelig filnavn brukes aldri som lagringsnøkkel [6]. Hvis det lagres som visningsnavn, kan det inneholde personnavn. Et alternativ er å generere visningsnavnet fra dokumenttypen.

### Lagring

PostgreSQL `bytea` (`STORAGE EXTERNAL`) i egen tabell bak et `FileStorage`-grensesnitt. Ingen ny tjeneste. SeaweedFS er planlagt utvei.

---

## Utkast til beslutningsnotat (00XX: Bilder og dokumenter på annonser)

**Problem**
Annonser trenger bilder og dokumenter. Opplastede filer er upålitelig input: skadevare, aktivt innhold, dekompresjonsbomber og polyglotter. De bærer også personopplysninger, både i metadata (GPS, navn) og i innholdet (signaturer). Admin godkjenner alt og må ikke oversvømmes. Beslutning 0009 krever innebygd personvern, rammeverksmekanismer, enkleste løsning og vurdering av eksterne tjenester.

**Alternativer**
1. *Formater:* (a) JPEG/PNG + PDF; (b) i tillegg WebP (TwelveMonkeys, BSD-3); (c) i tillegg HEIC (libheif, LGPL/native).
2. *Sanering:* (a) omkoding av bilder + validering/avvisning av PDF; (b) full CDR (rastrering av PDF); (c) bare lagring uten behandling.
3. *Skadevareskanning:* (a) ClamAV-container (GPLv2, 3–4 GB RAM); (b) ingen, der vi stoler på omkoding og PDF-avvisning; (c) ekstern skannetjeneste (krever vurdering av personopplysninger og behandlingssted).
4. *Lagring:* (a) PostgreSQL `bytea`; (b) Large Objects; (c) S3-kompatibel (SeaweedFS Apache 2.0 / Garage AGPLv3; MinIO utgår); (d) filsystemvolum.
5. *Innholdsvurdering:* (a) biblioteker + admin; (b) LLM/visjon i tillegg.

**Valg (forslag)**
1a, 2a, 3a som egen compose-profil som er påslått i demo hvis maskinen har nok RAM (ellers 3b som dokumentert prototypebegrensning), 4a bak et `FileStorage`-grensesnitt og 5a. Tika `tika-core` (Apache 2.0), PDFBox (Apache 2.0) og metadata-extractor (Apache 2.0) legges til som avhengigheter. Vaadin Upload (Apache 2.0) med `toTempFile`.

**Konsekvens**
- Positivt: ingen ny lagringstjeneste, transaksjonell sletting, én backup, metadata fjernet som standard, dokumenter lukket som standard, tidlig avvisning i opplastingsdialogen.
- Negativt: iPhone-brukere kan møte HEIC-avvisning (må testes), PDF-er mister skjemaer, lenkehandlinger og innebygde filer, og kan fortsatt være risikable å åpne lokalt uten full CDR, ClamAV øker RAM-kravet i Compose, og databasen og backupen vokser med filene.
- Oppfølging: nytt notat ved LLM-vurdering, ved bytte til objektlagring eller ved ekstern skannetjeneste. Oppdater `docs/security-baseline.md` punkt 3 når dette er implementert.

---

## Åpne spørsmål til produkteier

1. Skal dokumenter på publiserte annonser være åpne for alle, eller bare for innloggede kjøpere? Anbefalt standard er innlogget.
2. Skal ClamAV være med i den lokale demoen (3–4 GB RAM ekstra), eller skal manglende skanning aksepteres som dokumentert prototypebegrensning?
3. Hvor lenge etter at en annonse er solgt, trukket eller avvist skal vedlegg beholdes (forslag: 30 dager), og hvor lang skal backup-rotasjonen være?
4. Skal mobilfoto av papirdokumenter (JPEG) godtas som «dokument», eller skal vi kreve PDF (telefonens skannefunksjon)?
5. Er grensene (15/20 MB, 12 bilder, 10 dokumenter, 100 sider, minst 800 px) riktige for bransjen?
6. Skal selger bekrefte ved opplasting at unødvendige navn og signaturer er sladdet?
7. Skal originalfilnavn vises, eller skal visningsnavnet genereres fra dokumenttype?
8. Er en LLM-forhåndsvurdering av bilder ønsket i en senere fase? Den krever eget notat, databehandleravtale og EU-leverandør.

---

## Kilder

1. Java SE 24, `javax.imageio` package summary: https://docs.oracle.com/en/java/javase/24/docs/api/java.desktop/javax/imageio/package-summary.html
2. TwelveMonkeys ImageIO (README, BSD-3-Clause): https://github.com/haraldk/TwelveMonkeys
3. libheif (LGPL, eksempler MIT, x265 GPL): https://github.com/strukturag/libheif
4. MDN, Image file type and format guide: https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Formats/Image_types
5. Sekundærkilder om iOS/Safari og HEIC (ikke primærkilder): https://developer.apple.com/forums/thread/743049 · https://shkspr.mobi/blog/2020/12/coping-with-heic-in-the-browser/
6. OWASP File Upload Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
7. Apache Tika, Content Detection: https://tika.apache.org/3.2.0/detection.html
8. Apache Tika repo/LICENSE (Apache 2.0): https://github.com/apache/tika
9. Apache Tika forside (versjoner og EOL): https://tika.apache.org/
10. Apache Tika Security: https://tika.apache.org/security.html
11. Vaadin Flow `UploadHandler.java` (standardgrenser): https://github.com/vaadin/flow/blob/main/flow-server/src/main/java/com/vaadin/flow/server/streams/UploadHandler.java
12. Spring Boot Common Application Properties (multipart-standarder): https://docs.spring.io/spring-boot/appendix/application-properties/index.html
13. Vaadin, How to handle file uploads: https://vaadin.com/docs/latest/building-apps/forms-data/handle-uploads
14. ClamAV repo (GPLv2): https://github.com/Cisco-Talos/clamav
15. ClamAV Docker: https://docs.clamav.net/manual/Installing/Docker.html
16. clamd(8) man page, INSTREAM: https://github.com/Cisco-Talos/clamav/blob/main/docs/man/clamd.8.in
17. `clamd.conf.sample`: https://github.com/Cisco-Talos/clamav/blob/main/etc/clamd.conf.sample
18. ClamAV Scanning: https://docs.clamav.net/manual/Usage/Scanning.html
19. PDFBox `PDDocumentNameDictionary`: https://javadoc.io/static/org.apache.pdfbox/pdfbox/3.0.5/org/apache/pdfbox/pdmodel/PDDocumentNameDictionary.html
20. PDFBox `PDDocumentCatalog`: https://javadoc.io/static/org.apache.pdfbox/pdfbox/3.0.5/org/apache/pdfbox/pdmodel/PDDocumentCatalog.html
21. PDFBox `PDDocument`: https://javadoc.io/static/org.apache.pdfbox/pdfbox/3.0.5/org/apache/pdfbox/pdmodel/PDDocument.html
22. Apache PDFBox forside (lisens, 3.0.8, funksjoner): https://pdfbox.apache.org/
23. MDN, CSP `sandbox`: https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/sandbox
24. PDFBox 3.0 migration guide (`Loader`, cache): https://pdfbox.apache.org/3.0/migration.html
25. PDFBox FAQ: https://pdfbox.apache.org/3.0/faq.html
26. Java SE 24 `ImageReader`: https://docs.oracle.com/en/java/javase/24/docs/api/java.desktop/javax/imageio/ImageReader.html
27. Apache Commons Imaging: https://commons.apache.org/proper/commons-imaging/
28. Apache Commons Imaging Security: https://commons.apache.org/proper/commons-imaging/security.html
29. Datatilsynet, Personopplysninger: https://www.datatilsynet.no/rettigheter-og-plikter/personopplysninger/
30. metadata-extractor (Apache 2.0, bare lesing): https://github.com/drewnoakes/metadata-extractor
31. Java SE 24, JPEG Metadata Format Specification: https://docs.oracle.com/en/java/javase/24/docs/api/java.desktop/javax/imageio/metadata/doc-files/jpeg_metadata.html
32. GDPR (forordning (EU) 2016/679), EUR-Lex: https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32016R0679
33. Personopplysningsloven § 1: https://lovdata.no/dokument/NL/lov/2018-06-15-38
34. Datatilsynet, Innebygd personvern og personvern som standard: https://www.datatilsynet.no/rettigheter-og-plikter/virksomhetenes-plikter/innebygd-personvern-og-personvern-som-standard/
35. OpenCV LICENSE (Apache 2.0): https://github.com/opencv/opencv/blob/4.x/LICENSE
36. Claude Vision (tokenkostnad, metadata, sletting): https://platform.claude.com/docs/en/build-with-claude/vision
37. Mistral Help Center, datalagring: https://help.mistral.ai/en/articles/347629-where-do-you-store-my-data-or-my-organization-s-data
38. OpenAI, Data residency for the API: https://help.openai.com/en/articles/10503543-data-residency-for-the-openai-api
39. Claude Platform, Data residency: https://platform.claude.com/docs/en/manage-claude/data-residency
40. W3C, Understanding SC 1.1.1 Non-text Content (WCAG 2.2): https://www.w3.org/WAI/WCAG22/Understanding/non-text-content.html
41. PostgreSQL 17, TOAST: https://www.postgresql.org/docs/17/storage-toast.html
42. PostgreSQL 17, Large Objects Introduction: https://www.postgresql.org/docs/17/lo-intro.html
43. PostgreSQL 17, vacuumlo: https://www.postgresql.org/docs/17/vacuumlo.html
44. PostgreSQL 17, Large Objects Implementation Features: https://www.postgresql.org/docs/17/lo-implementation.html
45. MinIO repo (arkivert, AGPLv3, bare kildekode): https://github.com/minio/minio
46. SeaweedFS repo (Apache 2.0): https://github.com/seaweedfs/seaweedfs
47. Garage-repo (hovedrepo): https://git.deuxfleurs.fr/Deuxfleurs/garage
48. Garage GitHub-speil, LICENSE (AGPL-3.0): https://github.com/deuxfleurs-org/garage/blob/main-v2/LICENSE
49. Garage forside (formål, NGI-finansiering): https://garagehq.deuxfleurs.fr/
50. Garage S3-kompatibilitet: https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-compatibility/
51. Vaadin, Downloads (`DownloadHandler`): https://vaadin.com/docs/latest/flow/advanced/downloads
52. Vaadin Upload LICENSE (Apache 2.0): https://github.com/vaadin/flow-components/blob/main/vaadin-upload-flow-parent/LICENSE
53. Vaadin, Modular Upload: https://vaadin.com/docs/latest/components/upload/modular-upload
54. Vaadin Upload, File Handling: https://vaadin.com/docs/latest/components/upload/file-handling
55. Vaadin Flow `InMemoryUploadHandler.java`: https://github.com/vaadin/flow/blob/main/flow-server/src/main/java/com/vaadin/flow/server/streams/InMemoryUploadHandler.java
56. Vaadin, Using UploadHandler to Receive an Incoming Data Stream: https://vaadin.com/docs/latest/flow/advanced/upload-resources
57. Bokføringsloven: https://lovdata.no/dokument/NL/lov/2004-11-19-73
58. Bokføringsloven § 13: https://lovdata.no/lov/2004-11-19-73/§13
59. Bokføringsforskriften §§ 7-4, 7-5: https://lovdata.no/dokument/SF/forskrift/2004-12-01-1558
60. Kjøpsloven § 32: https://lovdata.no/lov/1988-05-13-27/§32
61. Skatteforvaltningsloven § 7-11: https://lovdata.no/lov/2016-05-27-14/§7-11
62. Skatteetaten, Om DPI: https://www.skatteetaten.no/en/business-and-organisation/reporting-and-industries/third-part-data/andre-bransjer/digital-plattforminformasjon-dpi/om-ordningen/
63. Skatteforvaltningsforskriften § 7-11-3 m.fl.: https://lovdata.no/dokument/SF/forskrift/2016-11-23-1360/%C2%A77-11-3
64. Skatteetaten, høring om opplysningsplikt om salgs- og kjøpstransaksjoner: https://www.skatteetaten.no/en/rettskilder/type/horinger/opplysningsplikt-salgs-og-kjopstransaksjoner
