package no.fagskolen.prosjekt.marketplace.affiliations;

import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;

import java.util.List;

/**
 * Tilknytninger mellom personer og virksomheter (userflow 11).
 */
public interface Affiliations {

    /**
     * Registrerer en virksomhet for personen. Finnes virksomheten allerede, brukes
     * den uendret. Tilknytningen opprettes med status venter, eller den eksisterende
     * returneres hvis personen allerede er knyttet til virksomheten.
     */
    Affiliation register(String personSubject, OrganisationNumber organisationNumber, String name, String location);

    List<Affiliation> findFor(String personSubject);
}
