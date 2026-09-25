# R1 – Innlogging og e-postbekreftelse for «Selg utstyr» (Keycloak 26.7)

Research-ticket: vardaasen/prosjekt#13 · Undersøkt 2026-09-25 · Kun primærkilder (keycloak.org, keycloak/keycloak på GitHub, p2-inc/keycloak-magic-link, Mailgun/Sinch sine egne sider, NIST).

Kildeforkortelser brukt under:

- **[ADMIN]** Keycloak Server Administration Guide (latest = 26.7.x): https://www.keycloak.org/docs/latest/server_admin/index.html
- **[DEV]** Keycloak Server Developer Guide: https://www.keycloak.org/docs/latest/server_development/index.html
- **[RN]** Keycloak Release Notes: https://www.keycloak.org/docs/latest/release_notes/index.html
- **[UPG]** Keycloak Upgrading Guide: https://www.keycloak.org/docs/latest/upgrading/index.html
- **[SRC]** Keycloak-kildekode på tag `26.7.4`: https://github.com/keycloak/keycloak/tree/26.7.4

## Sammendrag

- **Keycloak 26.7.4 finnes** og er nyeste versjon på nedlastingssiden ([keycloak.org/downloads](https://www.keycloak.org/downloads)).
- **Innebygd selvregistrering med e-postverifisering dekker behovet** uten utvidelser. Fra og med 26.7.0 er rekkefølgen «e-post først»: når *User registration* og *Verify email* er på, viser registreringsskjemaet ikke passordfelt. Brukeren bekrefter e-posten først, og velger deretter passord eller passkey ([UPG, 26.7.0](https://www.keycloak.org/docs/latest/upgrading/index.html#verify-email-required-before-credentials-setup-during-user-self-registration)). Det er i praksis det produktet ber om.
- **Keycloak 26.x har ingen innebygd passordløs e-postinnlogging** (ingen magisk lenke, ingen engangskode på e-post). Det finnes ingen slik authenticator i kildekoden for 26.7.4, og release notes for 26.0–26.7 nevner ingen. Det innebygde passordløse alternativet er **passkeys**, som er «supported» fra 26.4.0 ([RN](https://www.keycloak.org/docs/latest/release_notes/index.html)).
- **p2-inc/keycloak-magic-link er lisensiert under Elastic License 2.0** (ikke en OSI-godkjent åpen kildekode-lisens). Den bygger på Keycloaks interne SPI-er, som Keycloak selv sier «may change without notice». Den anbefales ikke som standard.
- **XOAUTH2 for SMTP finnes i Keycloak fra 26.2.0** (ikke 26.4), med *client credentials grant* ([RN 26.2.0](https://www.keycloak.org/docs/latest/release_notes/index.html#token-based-authentication-for-smtp-xoauth2)). **Mailguns SMTP støtter ikke XOAUTH2.** EU-serveren annonserer bare `AUTH PLAIN LOGIN`, og dokumentasjonen nevner bare SMTP-brukernavn og -passord. Da passer de ikke sammen. Anbefalingen er SMTP-credentials fra Mailgun, lagret som hemmelighet eller vault-referanse i Keycloak.
- **Mailgun har en EU-region.** Meldingsdata forlater ikke regionen, men noen kontodata replikeres globalt. Hvilket land EU-regionen ligger i, er ikke oppgitt på sidene jeg fikk lest.

**Anbefaling (kort):** Bruk innebygd registrering, *Email as username* og *Verify email* på Keycloak 26.7.4, med passkeys slått på og passord som reserve. SMTP går via Mailgun EU med SMTP-credentials. Ingen tredjeparts-utvidelse. Identity brokering (Entra ID og Google) kan legges til senere som eget beslutningsnotat.

---

## 1. Innebygd selvregistrering med e-postverifisering

### Slik fungerer det

- **Slå på registrering:** *Realm settings → Login → User registration = ON*. Da vises en «Register»-lenke på innloggingssiden ([ADMIN – Enabling user registration](https://www.keycloak.org/docs/latest/server_admin/index.html#proc-enabling-user-registration_server_administration_guide)).
- **E-postverifisering:** *Verify email* er en realm-bryter i samme fane. Den legger på den påkrevde handlingen `VERIFY_EMAIL`, som sender en lenke brukeren må klikke før innlogging tillates ([ADMIN – Required actions](https://www.keycloak.org/docs/latest/server_admin/index.html#con-required-actions_server_administration_guide), [ADMIN – Update email workflow](https://www.keycloak.org/docs/latest/server_admin/index.html#_update-email-workflow)).
- **Ny rekkefølge fra 26.7.0:** Når selvregistrering og *Verify email* er på samtidig, viser registreringsskjemaet ikke passordfelt. Brukeren fyller inn profilen, bekrefter e-posten og setter deretter opp passord eller eventuelt OTP eller passkey. Den gamle oppførselen kan slås på igjen med *Always set password on register form* (på *Password validation* i registreringsflyten), men det valget er «deprecated» ([UPG – Migrating to 26.7.0](https://www.keycloak.org/docs/latest/upgrading/index.html#verify-email-required-before-credentials-setup-during-user-self-registration), [ADMIN – Clarification on verify email](https://www.keycloak.org/docs/latest/server_admin/index.html#con-user-registration_server_administration_guide)). I tillegg ble *Configure OTP* og *Update password* flyttet slik at de kommer etter *Verify Email* ([UPG 26.7.0](https://www.keycloak.org/docs/latest/upgrading/index.html#configure-totp-and-update-password-required-actions-moved-after-verify-email)).
- **Anbefalt i dokumentasjonen:** Slå også på *Forgot password*, slik at en bruker som ikke fullførte verifiseringen kan komme videre senere ([ADMIN – Clarification on verify email](https://www.keycloak.org/docs/latest/server_admin/index.html#con-user-registration_server_administration_guide)).

### Realm-innstillinger som er relevante

| Innstilling | Hvor | Merknad / kilde |
| --- | --- | --- |
| User registration | Realm settings → Login | [ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#proc-enabling-user-registration_server_administration_guide) |
| Verify email | Realm settings → Login | Utløser `VERIFY_EMAIL` ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#_update-email-workflow)) |
| Email as username / Login with email | Realm settings → Login | Overstyrer oppsettet i brukerprofilen for `username`/`email` ([ADMIN – User profile](https://www.keycloak.org/docs/latest/server_admin/index.html#managing-the-user-profile)) |
| Forgot password | Realm settings → Login | Anbefales sammen med Verify email ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#con-user-registration_server_administration_guide)) |
| Brukerprofil (User profile) | Realm settings → User profile | Standard er `username`, `email`, `firstName`, `lastName`. `username`/`email` kan ikke fjernes, de andre kan endres ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#managing-the-user-profile)). Gir **dataminimering**: fornavn/etternavn kan gjøres valgfrie eller fjernes. |
| Email Verification-levetid | Realm settings → Sessions/Tokens (*Timeouts*) | Egen timeout for verifiseringslenker. Ellers gjelder *User-Initiated Action Lifespan*, som har standardverdi 300 s ([ADMIN – Timeouts](https://www.keycloak.org/docs/latest/server_admin/index.html#_timeouts), [SRC Constants.java](https://github.com/keycloak/keycloak/blob/26.7.4/server-spi-private/src/main/java/org/keycloak/models/Constants.java)) |
| Resend-intervall | Authentication → Required actions → Verify Email | Ny e-post kan som standard sendes hvert 30. sekund, konfigurerbart (fra 26.4.0) ([UPG 26.4.0](https://www.keycloak.org/docs/latest/upgrading/index.html)) |
| Terms and conditions | Registreringsflyt / Required actions | Valgfritt ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#proc-requiring-tac-agreement-at-registration_server_administration_guide)) |
| reCAPTCHA | Registreringsflyt | Fins innebygd, men sender data til Google. Krever eget beslutningsnotat ([ADMIN – Enabling reCAPTCHA](https://www.keycloak.org/docs/latest/server_admin/index.html)) |

### Krav: SMTP

Keycloak må ha SMTP satt opp for å sende verifiserings-e-post (*Realm settings → Email*). Der settes Host, Port, Encryption, Authentication og Authentication Type (`password` eller `token`). Passordet kan referere til en ekstern vault ([ADMIN – Configuring email for a realm](https://www.keycloak.org/docs/latest/server_admin/index.html#_email)).

### Sikkerhetsdetaljer i 26.x

- Fra 26.0.0 kan en innlogget bruker ikke fullføre en verifiseringslenke som tilhører en annen konto. Et forsøk på å verifisere en allerede verifisert e-post gir hendelsen `EMAIL_ALREADY_VERIFIED`, som kan brukes til å oppdage lekkede lenker ([RN 26.0.0](https://www.keycloak.org/docs/latest/release_notes/index.html)).

### Brukeropplevelse (slik dokumentasjonen beskriver den)

1. Brukeren klikker «Selg utstyr». Appen krever innlogging og sender videre til Keycloak (OIDC + PKCE).
2. Brukeren klikker «Register» og oppgir e-post (og eventuelt navn, hvis profilen krever det).
3. Keycloak sender en verifiseringslenke, og brukeren klikker den.
4. Brukeren velger passord eller passkey og sendes tilbake til appen.

Ukjent fra primærkilder: nøyaktig hva som skjer i den **første** nettleseren hvis lenken åpnes på en **annen enhet** (for eksempel skjema på laptop, lenke på mobil). Dette bør testes i demo-oppsettet. Det påvirker ikke kravet om å fortsette et utkast på tvers av enheter. Det løses ved at utkastet lagres server-side på bruker-ID, og brukeren logger inn på begge enhetene.

---

## 2. Passordløs e-postinnlogging

### 2a. Innebygd i Keycloak 26.x (til og med 26.7.4)

- **Ingen passordløs e-postinnlogging er innebygd:**
  - Kildetreet for tag `26.7.4` har ingen authenticator for e-post-OTP eller magisk lenke. Under `services/.../authentication/authenticators/` finnes blant annet `OTPFormAuthenticator` (TOTP/HOTP fra app), `UsernameForm`, `PasswordForm`, WebAuthn/passkeys-authenticatorer og broker-authenticatorer som `IdpEmailVerificationAuthenticator` (bare for kontolenking) ([SRC authenticators/browser](https://github.com/keycloak/keycloak/tree/26.7.4/services/src/main/java/org/keycloak/authentication/authenticators/browser)).
  - Release notes for 26.0.0–26.7.0 nevner ikke e-post-OTP, magisk lenke eller passordløs e-post ([RN](https://www.keycloak.org/docs/latest/release_notes/index.html)). Høydepunktene i 26.7.0 er SCIM (preview), multi-cluster HA, proxy-blueprints og step-up for SAML.
  - Ingen åpne eller lukkede issues med denne tittelen ble funnet i keycloak/keycloak (søk: «email otp», «magic link», «passwordless email», 2026-09-25).
- **Innebygd passordløst alternativ – passkeys (WebAuthn):**
  - Preview i 26.0–26.3. Integrert i standard innloggingsskjemaer i 26.3.0. «Supported» i 26.4.0 ([RN 26.3.0/26.4.0](https://www.keycloak.org/docs/latest/release_notes/index.html)).
  - Slås på under *Authentication → Policies → Webauthn Passwordless Policy → Enable Passkeys*. Både conditional UI (autofill) og modal UI støttes ([ADMIN – Passkeys](https://www.keycloak.org/docs/latest/server_admin/index.html#passkeys_server_administration_guide)).
  - Synkroniserte og enhetsbundne passkeys fungerer både på samme enhet og på tvers av enheter, men resultatet avhenger av brukerens miljø ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#passkeys_server_administration_guide)).
  - 26.7.0 la til *Discoverable credential* (required/preferred/discouraged) for bedre kompatibilitet med iCloud Keychain, Google Password Manager og 1Password ([RN 26.7.0](https://www.keycloak.org/docs/latest/release_notes/index.html)).
  - Med 26.7-flyten (§1) kan brukeren velge passkey direkte etter e-postverifisering ([UPG 26.7.0](https://www.keycloak.org/docs/latest/upgrading/index.html#verify-email-required-before-credentials-setup-during-user-self-registration)).

### 2b. p2-inc/keycloak-magic-link (Phase Two)

- **Hva den gjør** ([README](https://github.com/p2-inc/keycloak-magic-link)):
  - *Magic Link Authenticator*: e-post inn, lenke ut. Den kan opprette brukeren hvis den ikke finnes.
  - Lenken er et Action Token, så den kan åpnes på en annen enhet enn den som startet innloggingen.
  - *Magic Link Continuation*: siden som startet innloggingen poller hvert 5. sekund, og innloggingen fullføres der (standard utløp 10 min).
  - *Email OTP*: 6-sifret kode på e-post.
  - I tillegg: *Login Token*, *Account Activation* og Cloudflare Turnstile.
- **Lisens:** filen [`COPYING`](https://github.com/p2-inc/keycloak-magic-link/blob/main/COPYING) er **Elastic License 2.0**, og GitHub rapporterer lisensen som «Other/NOASSERTION».
  - Vilkårene forbyr blant annet å tilby programvaren til tredjeparter «as a hosted or managed service» med vesentlige deler av funksjonaliteten, og å omgå lisensnøkkel-funksjonalitet.
  - Lisensen er ikke en OSI-lisens. Det bryter med prosjektets «åpent først»-linje og krever beslutningsnotat.
- **Keycloak-versjoner:** README har ingen kompatibilitetsmatrise.
  - `pom.xml` på `main` bygger mot `keycloak.version` **26.6.3**, ikke 26.7.x ([pom.xml](https://github.com/p2-inc/keycloak-magic-link/blob/main/pom.xml)).
  - Støtte for 26.7.4 er **ikke bekreftet** fra primærkilde.
  - Issue [#202](https://github.com/p2-inc/keycloak-magic-link/issues/202) viser at bygget krasjet med `NoSuchMethodError` mot en Keycloak-patchversjon (26.6.2 mot 26.6.3). Det er et konkret eksempel på oppgraderingsrisiko.
- **Vedlikehold:** aktivt.
  - Siste utgivelse `v0.83` 2026-09-24, `v0.82` 2026-09-22 og `v0.81` 2026-09-21.
  - Releases publiseres til Maven Central ved hver merge til `main`. Repoet har 13 åpne issues og er ikke arkivert (GitHub API, 2026-09-25).
- **Token-egenskaper:**
  - Om lenken er engangs, styres av `canUseTokenRepeatedly()`, som returnerer tokenets `persistent`-flagg ([MagicLinkActionTokenHandler.java](https://github.com/p2-inc/keycloak-magic-link/blob/main/src/main/java/io/phasetwo/keycloak/magic/auth/magic/MagicLinkActionTokenHandler.java)).
  - REST-ressursen har standardverdiene `expiration_seconds` = 86400 (1 døgn) og `reusable` = `true` ([README](https://github.com/p2-inc/keycloak-magic-link)). De må strammes inn.

### 2c. Egen Authenticator SPI + Action Token SPI

- **Hva Keycloak tilbyr:**
  - *Authentication SPI* for egne authenticatorer i flytene ([DEV – Authentication SPI](https://www.keycloak.org/docs/latest/server_development/index.html#_auth_spi)).
  - *Action Token Handler SPI*. Et action token er en JWT signert med realmens aktive nøkkel, med feltene `typ`, `iat`/`exp`, `sub`, `azp`, `iss`, `aud`, valgfri `asid` og valgfri `nonce`.
  - Handleren kan kreve engangsbruk. Brukte tokens avvises, og tokenet ugyldiggjøres når autentiseringsflyten er ferdig.
  - Hvis lenken åpnes i en nettleser uten matchende autentiseringssesjon, opprettes en ny sesjon ([DEV – Action Token Handler SPI](https://www.keycloak.org/docs/latest/server_development/index.html#_action_token_handler_spi)).
- **Stabilitet:** I 26.7.4 er både `AuthenticatorSpi`, `ActionTokenHandlerSpi`, `EmailSenderSpi` og `EmailTemplateSpi` merket `isInternal() = true` ([AuthenticatorSpi.java](https://github.com/keycloak/keycloak/blob/26.7.4/server-spi-private/src/main/java/org/keycloak/authentication/AuthenticatorSpi.java), [ActionTokenHandlerSpi.java](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/authentication/actiontoken/ActionTokenHandlerSpi.java)). Keycloak logger da advarselen «This SPI is internal and may change without notice» ([ServicesLogger.java](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/services/ServicesLogger.java)).
- **Innsats og vedlikehold (vurdering, ikke kildebelagt):** Vi må lage og vedlikeholde en egen Keycloak-provider-JAR med skjema/tema, e-postmal, tester og en Docker-byggsteg. Den må re-verifiseres ved hver Keycloak-oppgradering, også patch-versjoner (jf. #202 over). Dette er et nytt Maven-modul/artefakt utenfor Spring-appen.

### 2d. Engangskode på e-post

- **Ikke innebygd** i Keycloak 26.7.4 (se 2a). Den er tilgjengelig via p2-inc (ELv2) eller egen SPI.
- **NIST SP 800-63B-4 §3.1.3.1:** «Email SHALL NOT be used for out-of-band authentication». Begrunnelsen er at e-postkontoen kan være beskyttet av bare et passord, at e-post kan avlyttes underveis eller på mellomliggende servere, og at den kan omdirigeres via DNS-spoofing. Koder som sendes for å *validere* en e-postadresse eller for gjenoppretting regnes derimot ikke som autentisering ([NIST SP 800-63B-4](https://pages.nist.gov/800-63-4/sp800-63b.html)). Det samme resonnementet gjelder magiske lenker (vurdering).

### Sammenligning (spørsmål 2)

| Kriterium | Innebygd: registrering + Verify email + passord/passkey | Innebygd: passkeys (etter verifisert e-post) | p2-inc magic link / e-post-OTP | Egen SPI (lenke eller kode) |
| --- | --- | --- | --- | --- |
| Enkelhet | Høy: kun realm-konfig ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#proc-enabling-user-registration_server_administration_guide)) | Høy: én bryter ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#passkeys_server_administration_guide)) | Middels: JAR i `providers/`, egen browser-flow, versjonstilpasning | Lav: egen kode, tema, e-postmal, tester |
| Friksjon for bruker | Middels: e-post, klikk, velg passord/passkey første gang | Lav etter oppsett, avhenger av enhet/nettleser | Lav: ingen hemmelighet å huske | Lav |
| Phishing | Passord kan phishes | Phishing-resistent (WebAuthn, origin-bundet) | Lenke/kode kan videresendes eller lures ut; e-post frarådes som autentisator ([NIST](https://pages.nist.gov/800-63-4/sp800-63b.html)) | Som p2-inc |
| Avlytting av lenke | Kun verifiseringslenke, kort levetid (standard 300 s), `EMAIL_ALREADY_VERIFIED`-hendelse ([RN 26.0.0](https://www.keycloak.org/docs/latest/release_notes/index.html)) | Ingen e-post i innlogging | E-posten *er* innloggingen. API-standard 1 døgn og gjenbrukbar ([README](https://github.com/p2-inc/keycloak-magic-link)) må strammes | Styres selv (engangs via SPI ([DEV](https://www.keycloak.org/docs/latest/server_development/index.html#_action_token_handler_spi))) |
| Engangsbruk | Ja, håndtert av Keycloak | Ikke relevant | Konfigurerbart (`persistent`/`reusable`) | Mulig via `SingleUseObjectProvider` ([DEV](https://www.keycloak.org/docs/latest/server_development/index.html#_action_token_handler_spi)) |
| Personvern | Kun e-post (+valgfritt navn); e-post sendes via SMTP-leverandør én gang | Ingen biometri forlater enheten (WebAuthn-prinsipp); ingen ekstra e-post | Hver innlogging sender e-post via tredjepart (flere behandlinger) | Som p2-inc |
| Lisens | Apache 2.0 (Keycloak) | Apache 2.0 | **Elastic License 2.0** ([COPYING](https://github.com/p2-inc/keycloak-magic-link/blob/main/COPYING)) | Egen kode |
| Oppgraderings-/vedlikeholdsrisiko | Lav: standardfunksjon | Lav: «supported» fra 26.4 | Høy: interne SPI-er, bygger mot 26.6.3, brudd på patch-nivå (#202) | Høy: interne SPI-er «may change without notice» ([ServicesLogger](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/services/ServicesLogger.java)) |

---

## 3. Identity brokering (Entra ID og Google)

### Generelt

- Keycloak fungerer som identity broker. Eksterne IdP-er legges til under *Identity Providers*. Innebygde sosiale IdP-er inkluderer blant annet Google og Microsoft, og i tillegg finnes generiske OIDC/SAML/OAuth 2.0-brokere ([ADMIN – Integrating identity providers](https://www.keycloak.org/docs/latest/server_admin/index.html#_identity_broker)).
- **Viktige felles innstillinger** ([ADMIN – General configuration](https://www.keycloak.org/docs/latest/server_admin/index.html#_general-idp-config)):
  - *Trust Email*: e-posten fra IdP-en regnes som verifisert. Fra 26.3.0 respekteres OIDC-claimet `email_verified` når *Sync Mode = FORCE* ([RN 26.3.0](https://www.keycloak.org/docs/latest/release_notes/index.html)).
  - *Store Tokens*: av som standard. Bør forbli av (dataminimering).
  - *Hide on Login Page*, *Account Linking Only*, *Sync Mode* (legacy/import/force) og *Verify essential claim*.
- **Mappere:** IdP-mappere bestemmer hvilke claims som importeres til den lokale brukeren, per mapper med *Sync Mode Override* ([ADMIN – Mapping claims](https://www.keycloak.org/docs/latest/server_admin/index.html#_mappers)). Dataminimering oppnås ved å be om minst mulig scope og ikke legge til mappere ut over e-post.

### Microsoft Entra ID

- **Oppsett:** Registrer appen under *App registrations* i Azure, sett redirect-URI, lag en klienthemmelighet, og legg klient-ID og hemmelighet inn i Keycloak ([ADMIN – Microsoft](https://www.keycloak.org/docs/latest/server_admin/index.html#_microsoft)).
- **Slik fungerer provideren i 26.7.4** ([MicrosoftIdentityProvider.java](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/social/microsoft/MicrosoftIdentityProvider.java)):
  - Endepunkter `login.microsoftonline.com/{tenant}/oauth2/v2.0/...`. Tenant er `common` hvis ikke satt.
  - Tenant kan settes til en konkret tenant, eller til `organizations` (kun jobbkontoer) eller `consumers`.
  - Ved konkret tenant avvises tokens der `tid` ikke matcher.
  - Standard scope er `User.read`. Profilen hentes fra Microsoft Graph `/v1.0/me`.
  - Importerte felter: `id`, `mail` (faller tilbake til `userPrincipalName` hvis det er en gyldig e-post), `givenName` og `surname`.
- **For B2B:** Tenant `organizations` begrenser til jobbkontoer. Alternativt kan generisk OIDC-broker med tenant-spesifikk utsteder brukes per kunde, eller Keycloak *Organizations* med domene-til-IdP-ruting ([ADMIN – Organizations](https://www.keycloak.org/docs/latest/server_admin/index.html#managing-an-organization)).
- **Ikke bekreftet fra primærkilde i denne researchen:** om Graph-feltet `mail` er verifisert av Microsoft. Keycloak-provideren sender ikke `email_verified`. Anbefaling: *Trust Email = OFF* for Microsoft til dette er avklart. Da må brukeren verifisere e-posten i Keycloak.

### Google

- **Oppsett:** OAuth consent screen (External) og OAuth-klient av typen «Web application» i Google Cloud Console. Standard scopes er `openid profile email` ([ADMIN – Google](https://www.keycloak.org/docs/latest/server_admin/index.html#_google)).
- **Dataminimering:** Scopes kan reduseres i *Default Scopes*. Om `profile` kan fjernes uten at Keycloak-flyten krever navn, avhenger av brukerprofilen (§1).

### Kontolenking mot eksisterende e-post

Standardflyten *First Broker Login* ([ADMIN – First login flow](https://www.keycloak.org/docs/latest/server_admin/index.html#_identity_broker_first_login)):

- **Review Profile:** brukeren ser importerte data.
- **Create User If Unique:** lager en ny lokal bruker hvis e-post/brukernavn er ledig. Ellers går flyten videre til *Handle Existing Account*.
- **Confirm Link Existing Account:** brukeren bekrefter koblingen. Deretter bevises eierskap med **Verify Existing Account By Email** (krever SMTP) eller **Verify Existing Account By Re-authentication**.
- **Advarsel:** Keycloak skriver at automatisk lenking på e-post er «a potential security hole». *Automatically Set Existing User* er «dangerous in a generic environment where users can register themselves» ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#automatically-link-existing-first-login-flow)). Med åpen selvregistrering skal auto-lenking **ikke** brukes.

### Personopplysninger som flyter fra ekstern IdP

- **Microsoft:** Graph `/me` med `User.read`. Keycloak leser `id`, `mail`/`userPrincipalName`, `givenName` og `surname` ([SRC](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/social/microsoft/MicrosoftIdentityProvider.java)). Hele `/me`-svaret mottas av Keycloak. Det som lagres, styres av provider og mappere.
- **Google:** claims fra `openid profile email` ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#_google)).
- **Sesjonsnotater:** `identity_provider` og `identity_provider_identity` er tilgjengelige og kan legges i tokens via mapper. De bør ikke mappes ut uten behov ([ADMIN – Available user session data](https://www.keycloak.org/docs/latest/server_admin/index.html#_mappers)).
- **Eksterne tjenester:** Begge IdP-ene er tredjeparter utenfor EU-kontroll og krever egne beslutningsnotater etter 0009.

---

## 4. XOAUTH2 for utgående SMTP

- **Keycloak:** «Token based authentication for SMTP (XOAUTH2)» kom i **26.2.0**. Tokenet hentes med *Client Credentials Grant* ([RN 26.2.0](https://www.keycloak.org/docs/latest/release_notes/index.html#token-based-authentication-for-smtp-xoauth2)).
  - Konfig: *Authentication Type = token* med *Auth Token URL*, *Scope*, *ClientId* og *Client Secret*. Hemmeligheten kan være en vault-referanse ([ADMIN – Configuring email](https://www.keycloak.org/docs/latest/server_admin/index.html#_email)).
  - Implementasjonen poster `grant_type=client_credentials` og cacher tokenet per realm ([TokenAuthEmailAuthenticator.java](https://github.com/keycloak/keycloak/blob/26.7.4/services/src/main/java/org/keycloak/email/TokenAuthEmailAuthenticator.java)).
  - Dokumentasjonen viser et verifisert oppsett for Microsoft 365. Google Mail støttes *ikke*, fordi Google ikke tillater klienthemmeligheter for client credentials. AWS SMTP støtter ikke XOAUTH2 ([ADMIN – XOAUTH2 with third-party vendors](https://www.keycloak.org/docs/latest/server_admin/index.html#configuration-for-microsoft-azure-and-office365)).
  - (Merk: en sekundær oppsummering oppga 26.4. Release notes plasserer funksjonen under 26.2.0.)
- **Mailgun:**
  - Dokumentasjonen beskriver bare SMTP-credentials (brukernavn = full adresse på domenet, passord satt eller generert per credential). Relé-guiden sier at en må bruke «SMTP Credentials, but not your Control Panel password». Hverken XOAUTH2 eller OAuth nevnes for SMTP ([Send via SMTP](https://documentation.mailgun.com/docs/mailgun/user-manual/sending-messages/send-smtp), [SMTP Relay](https://documentation.mailgun.com/docs/mailgun/user-manual/smtp-protocol/smtp-relay), [Credentials API](https://documentation.mailgun.com/docs/mailgun/api-reference/send/mailgun/credentials/post-v3-domains--domain-name--credentials)).
  - Direkte observasjon 2026-09-25: `EHLO` mot `smtp.eu.mailgun.org:587` etter STARTTLS annonserer `250-AUTH PLAIN LOGIN`, altså ingen `XOAUTH2`. Sertifikatet er utstedt til `*.eu.mailgun.org`.
  - Mailguns HTTP-API bruker API-nøkkel (401 ved «Invalid or missing API key») ([API overview](https://documentation.mailgun.com/docs/mailgun/api-reference/api-overview)).
- **Konsekvens:** Keycloaks XOAUTH2-støtte kan ikke brukes mot Mailgun. Alternativene er:
  1. **Mailgun SMTP-credentials som hemmelighet** (anbefalt): egen credential kun for Keycloak, med passordet i Keycloak-vault eller hemmelighet i Compose/miljø, ikke i realm-import-JSON. Dette er framework-mekanismen (*Authentication Type = password*, vault-referanse) ([ADMIN](https://www.keycloak.org/docs/latest/server_admin/index.html#_email)).
  2. **Mailgun HTTP-API:** krever egen `EmailSenderProvider`, men den SPI-en er intern ([EmailSenderSpi.java](https://github.com/keycloak/keycloak/blob/26.7.4/server-spi-private/src/main/java/org/keycloak/email/EmailSenderSpi.java)). Det gir samme vedlikeholdsbyrde som 2c og anbefales ikke.
  3. **En annen leverandør med XOAUTH2** (for eksempel Microsoft 365, som Keycloak dokumenterer): gir en annen databehandler og andre vilkår, og krever nytt beslutningsnotat.

---

## 5. Mailgun – EU-region

- **Regionvalg:** Mailgun er hostet i både EU og USA, og kunden velger region per sendedomene fra samme konto ([Mailgun Regions](https://www.mailgun.com/about/regions/)). EU-regionen ble lansert 2018-07-26 ([Mailgun-blogg](https://www.mailgun.com/blog/product/we-have-a-new-region-in-europe-yall/)).
- **Data som blir i regionen:** Meldingsdata «never leaves the region that it is processed by». Regionbundet er domenemetadata, meldinger, hendelseslogger, suppressions, mailinglister, tagger, statistikk, ruter og IP-er ([Regions](https://www.mailgun.com/about/regions/)).
- **Data som replikeres globalt:** kontoinformasjon, brukerkontoer, fakturadetaljer, API-nøkler og domenenavn ([Regions](https://www.mailgun.com/about/regions/)).
- **Endepunkter:**
  - API for EU: `https://api.eu.mailgun.net/` ([API overview](https://documentation.mailgun.com/docs/mailgun/api-reference/api-overview)).
  - SMTP-host for EU: `smtp.eu.mailgun.org` er observert direkte (se §4). Mailgun-dokumentasjonen jeg fikk lest viste bare `smtp.mailgun.org`. Hjelpesenter-artiklene returnerte HTTP 403, så EU-SMTP-hosten er **ikke bekreftet i offisiell dokumentasjon** i denne runden.
- **GDPR:** Mailgun (Sinch) har en DPA med EU standard contractual clauses og liste over underdatabehandlere for kunder ([Mailgun GDPR](https://www.mailgun.com/gdpr/), [Sinch DPA](https://sinch.com/legal/terms-and-conditions/other-sinch-terms-conditions/data-protection-agreement/)).
- **Ikke bekreftet:**
  - Hvilket EU-land eller datasenter EU-regionen ligger i.
  - Om EU-US Data Privacy Framework er relevant, fordi GDPR-siden ikke nevner det.
  - GDPR-siden nevner AWS, Rackspace, Softlayer og Google Cloud som infrastrukturleverandører, uten region.

---

## Anbefaling

Etter prinsippet om den **enkleste løsningen som oppfyller kravene** (0009):

0. **Forutsetning før selvregistrering slås på** i et miljø som er nåbart utenfra: misbruksvern må være på plass og testet. Det betyr rate limiting på registrering og på utsending av verifiserings-e-post, både globalt og per kilde og konto, før e-post sendes. Beslutningsnotat 0006 krever dette («Før registrering aktiveres må appen ha rate limiting …»), og det gjelder fortsatt etter 0007. Keycloaks intervall for ny utsending av verifiserings-e-post er bare en brukeropplevelseskontroll, ikke misbruksvern. Hvor begrensningen skal ligge (foran Keycloak i en reverse proxy, eller i Keycloak), avgjøres i eget beslutningsnotat (Aikido-funn på PR #31). I den lokale demoen, som bare er bundet til `127.0.0.1`, kan registrering være på.
1. **Keycloak 26.7.4 med kun innebygde mekanismer**, når forutsetning 0 er oppfylt:
   - *User registration* = ON, *Email as username* = ON, *Verify email* = ON og *Forgot password* = ON.
   - Brukerprofilen minimeres til e-post. Fornavn og etternavn gjøres valgfrie eller fjernes.
   - Med 26.7-flyten får brukeren: e-post → verifiser → velg passkey eller passord → tilbake til «Selg utstyr».
   - Utkastet opprettes først etter innlogging og eies av Keycloak-`sub`, noe som gir fortsettelse på tvers av enheter.
2. **Passkeys på** (*Enable Passkeys*), med passord som reserve. Dette gir passordløs og phishing-resistent innlogging uten e-post i hver innlogging, og uten tredjepartskode.
3. **SMTP:** Mailgun EU-domene via `smtp.eu.mailgun.org:587` (STARTTLS) med en dedikert SMTP-credential. Passordet lagres som vault-referanse eller hemmelighet, aldri i realm-import-JSON eller Git. XOAUTH2 er ikke mulig mot Mailgun.
4. **Ikke** ta inn p2-inc/keycloak-magic-link eller egen SPI nå (ELv2-lisens, interne SPI-er, NIST fraråder e-post som autentisator).
5. **Identity brokering (Entra `organizations` og Google)** tas som eget, senere beslutningsnotat, med *Trust Email* = OFF for Microsoft til `mail` er avklart, standard *First Broker Login* uten auto-lenking og *Store Tokens* = OFF.

**Dette ville endret anbefalingen:**

- Brukertester viser at passord/passkey-steget gir uakseptabelt frafall. Da bør e-post-OTP vurderes, helst hvis Keycloak får en innebygd variant, ellers med eget beslutningsnotat for ELv2.
- Keycloak får innebygd e-post-OTP eller magisk lenke i en senere versjon.
- Kunder krever SSO med egen Entra-tenant. Da flyttes brokering og *Organizations* fram.
- Mailgun-avtalen eller lokasjonen ikke godkjennes. Da velges en annen leverandør, og XOAUTH2 via Microsoft 365 blir et alternativ.
- Demo-testen viser at verifisering på tvers av enheter (skjema på laptop, lenke på mobil) gir en forvirrende opplevelse.

## Åpne spørsmål til produkteier

1. Er det akseptabelt at brukeren må velge passord eller passkey etter e-postverifisering, eller er «aldri passord, kun e-post» et absolutt krav? Det siste krever ikke-innebygd løsning.
2. Hvilke profilfelter trenger vi *ved registrering*: kun e-post, eller også navn eller firma? Firma/organisasjonsnummer kan eventuelt samles senere i appen.
3. Skal «Logg inn med Microsoft/Google» være med i første versjon, eller komme senere? Skal Microsoft begrenses til jobbkontoer (`organizations`)?
4. Hva er ønsket levetid for verifiseringslenken (standard 5 min)? Er lengre levetid ønsket for brukere som bytter enhet?
5. Er Mailgun (Sinch) godkjent som databehandler, gitt at kontodata replikeres globalt og EU-landet ikke er oppgitt? Hvem signerer DPA?
6. Skal registreringen ha bot-beskyttelse? Keycloaks innebygde er Google reCAPTCHA, som sender data til Google og krever beslutningsnotat.
7. Hvor lenge skal uverifiserte kontoer og ufullførte registreringer beholdes før sletting (lagringstid etter 0009)?
