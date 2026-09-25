# Havbruksbrukt

Havbruksbrukt er en B2B-markedsplass for brukt akvakulturutstyr. Konteksten
beskriver hvordan virksomheter publiserer, oppdager og handler utstyr, og
hvordan markedsplassen verifiserer partene og handelen. Selve betalingen
skjer utenfor markedsplassen.

## Parter og verifisering

**Person**:
Et menneske som logger inn på markedsplassen. En person handler aldri på egne
vegne, bare for en virksomhet gjennom en tilknytning.
_Avoid_: Bruker, konto, kunde

**Virksomhet**:
Den juridiske enheten som er part i en annonse, et bud og en handel,
identifisert med organisasjonsnummer.
_Avoid_: Firma, organisasjon, selskap, kunde

**Tilknytning**:
Koblingen som lar en person handle på vegne av en virksomhet. En person kan ha
tilknytninger til flere virksomheter.
_Avoid_: Medlemskap, ansettelse, rolle

**Verifisert tilknytning**:
En tilknytning som markedsplassadministratoren har kontrollert og som ikke er
trukket tilbake. Verifisering gjelder tilknytningen, ikke personen alene, og
er ikke varig i seg selv.
_Avoid_: Verifisert selger, godkjent bruker, sertifisert konto

**Selger**:
Virksomheten som tilbyr utstyr i en bestemt annonse. En rolle i en annonse
eller handel, ikke en kontotype.
_Avoid_: Leverandør, eier, selgerkonto

**Kjøper**:
Virksomheten som gir bud på en bestemt annonse. En rolle i et bud eller en
handel, ikke en kontotype.
_Avoid_: Kunde, bruker

**Markedsplassadministrator**:
En autorisert person som verifiserer tilknytninger, godkjenner annonser og
verifiserer handler, med en egen innlogging som aldri handler. Rollen er
forskjellig fra en Keycloak-plattformadministrator.
_Avoid_: Keycloak-admin, superbruker

## Annonser

**Annonse**:
En presentasjon av ett konkret tilbud om brukt utstyr, med
utstyrsopplysninger, pris, lokasjon, tilstand, selger og dokumentasjon.
Annonsen eies av virksomheten, ikke av personen som laget den.
_Avoid_: Produkt, oppføring, listing

**Utstyr**:
Det fysiske akvakulturutstyret som tilbys i en annonse. Utstyret beskrives i
annonsen og har ingen egen identitet på markedsplassen.
_Avoid_: Produkt, vare

**Utstyrskategori**:
En forretningsmessig inndeling av utstyr som gjør annonser enklere å finne og
sammenligne.
_Avoid_: Tagg, filtertype

**Lokasjon**:
Det geografiske området annonsen oppgir for utstyret.
_Avoid_: Adresse, region

**Tilstand**:
Selgers oppgitte vurdering av utstyrets stand ved publisering.
_Avoid_: Kvalitet, status

**Utkast**:
En annonse under utarbeidelse, eid av en virksomhet og bare synlig for
personer med verifisert tilknytning til den. Et utkast kan bare lages med
verifisert tilknytning.
_Avoid_: Kladd, upublisert annonse

**Annonsegodkjenning**:
Markedsplassadministratorens kontroll av en annonse før den blir offentlig.
Bare en virksomhet med verifisert tilknytning kan sende en annonse til
godkjenning.
_Avoid_: Moderering, review

**Publiseringsstatus**:
Annonsens livssyklus: utkast, til godkjenning, publisert, under handel eller
arkivert. Bare en publisert annonse, eller en under handel, er offentlig
synlig, og bare en publisert annonse tar imot bud.
_Avoid_: Synlighetsstatus, annonsetilstand

**Bilde**:
Et fotografi av utstyret i en annonse, som viser utstyret og dets tilstand.
Bilder publiseres sammen med annonsen.
_Avoid_: Foto, vedlegg, media

**Dokumentasjon**:
Materiale knyttet til en annonse som underbygger utstyrets tilstand, historikk
eller regelverksetterlevelse.
_Avoid_: Vedlegg, bevis

**Verifiseringsgrunnlag**:
Markedsplassadministratorens registrerte vurdering av dokumentasjonen til en
annonse, brukt ved annonsegodkjenning og handelsverifisering. Synlig for
administratoren og virksomheten, aldri publisert.
_Avoid_: Utstyrshistorikk, servicelogg

**Dokumentasjonsvurdering**:
Én vurdering i et verifiseringsgrunnlag: hva slags dokumentasjon det er, hvem
som har utstedt den, når, hvor lenge den gjelder og hva den viser.
_Avoid_: Sjekkliste, merknad

**Lagret annonse**:
En annonse en person har valgt å huske, sammenligne med andre og følge
endringer på. Den tilhører personen, ikke virksomheten.
_Avoid_: Favoritt, bokmerke, ønskeliste

## Handel og dialog

**Forespørsel**:
En henvendelse fra en virksomhet til selgeren om en bestemt annonse, uten
forpliktelse.
_Avoid_: Bestilling, kjøp, melding

**Bud**:
Et tilbud fra en kjøper om å kjøpe utstyret i en annonse til en bestemt pris.
Et bud kan gis før kjøperens tilknytning er verifisert.
_Avoid_: Tilbud, bestilling, ordre

**Handel**:
Avtalen som oppstår når selgeren aksepterer et bud. En handel er ikke
gjennomført før markedsplassadministratoren har verifisert den.
_Avoid_: Salg, ordre, transaksjon, kjøpsoppgjør

**Handelsverifisering**:
Markedsplassadministratorens kontroll av en handel: at begge parter har
verifisert tilknytning og at annonse og bud stemmer. Betalingen skjer utenfor
markedsplassen.
_Avoid_: Betalingsgodkjenning, oppgjør

## Utgående begreper

Brukes fortsatt i koden inntil den er migrert (se beslutningsnotat 0007), men
skal ikke brukes i nytt arbeid:

- **Selgersøknad** erstattes av tilknytning og annonsegodkjenning.
- **Selgerkonto** erstattes av tilknytning mellom person og virksomhet.
- **Verifisert selger** erstattes av verifisert tilknytning.
