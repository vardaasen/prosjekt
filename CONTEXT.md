# Havbruksbrukt

Havbruksbrukt er en B2B-markedsplass for brukt akvakulturutstyr. Konteksten
beskriver virksomhetenes publisering, oppdagelse og vurdering av utstyr; den
omfatter ikke selve kjøpsoppgjøret.

## Markedsplass

**Annonse**:
En offentlig presentasjon av ett konkret tilbud om brukt utstyr, med
utstyrsopplysninger, pris, lokasjon, tilstand, selger og dokumentasjon.
_Avoid_: Produkt, oppføring, listing

**Selger**:
Virksomheten som tilbyr utstyr gjennom en annonse.
_Avoid_: Leverandør, eier

**Selgerkonto**:
Koblingen mellom en innlogget OIDC-identitets stabile subject (`sub`) og en
selger. Kontoen eier fremtidige utkast og annonser; den inneholder ikke
passord eller andre identitetsleverandørdata.
_Avoid_: Brukerkonto, Keycloak-bruker

**Kjøper**:
Virksomheten som vurderer utstyr i markedsplassen, uavhengig av om den ender
med å kjøpe.
_Avoid_: Kunde, bruker

**Utstyr**:
Det fysiske akvakulturutstyret som tilbys i en annonse.
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

**Publiseringsstatus**:
Annonsens livssyklus i markedsplassen: utkast, publisert eller arkivert.
Bare en publisert annonse er offentlig synlig.
_Avoid_: Synlighetsstatus, annonsetilstand

**Utkast**:
En annonse under utarbeidelse. Den eies av én selgerkonto og er bare synlig
for denne selgeren til den publiseres.
_Avoid_: Kladd, upublisert annonse

## Tillit og dialog

**Dokumentasjon**:
Materiale knyttet til en annonse som underbygger utstyrets tilstand, historikk
eller regelverksetterlevelse.
_Avoid_: Vedlegg, bevis

**Verifisert selger**:
En selger som markedsplassen har merket som kontrollert. Merket uttrykker
selgerens verifiseringsstatus, ikke tilstanden til utstyret.
_Avoid_: Godkjent annonse, sertifisert utstyr

**Forespørsel**:
En kjøpers henvendelse til en selger om en bestemt annonse. Forespørsler
håndteres i den innloggede appflaten når denne flyten innføres.
_Avoid_: Bestilling, kjøp, melding
