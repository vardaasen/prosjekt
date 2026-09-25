package no.fagskolen.prosjekt.marketplace.people;

import java.util.Optional;

/**
 * Oppslag av en persons navn og e-post fra Keycloak-subject, for
 * markedsplassadministratorens vurdering (#18).
 */
public interface PersonDirectory {

    /** Tomt resultat når personen er ukjent eller oppslaget ikke kan gjøres. */
    Optional<PersonContact> lookup(String oidcSubject);
}
