package no.fagskolen.prosjekt.marketplace.affiliations;

import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationAuditEntry;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;

import java.util.List;
import java.util.UUID;

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

    /** Tilknytninger som venter på markedsplassadministratoren, eldste først. */
    List<Affiliation> findPending();

    /** Alle tilknytninger, nyeste først, for oversikten i administrasjonen. */
    List<Affiliation> findAll();

    /**
     * Beslutningene under kaster {@link IllegalStateException} hvis livssyklusen
     * ikke tillater overgangen, også når en annen administrator nettopp har avgjort
     * tilknytningen, og {@link IllegalArgumentException} hvis begrunnelse mangler.
     */
    Affiliation verify(UUID affiliationId, String administratorSubject);

    Affiliation reject(UUID affiliationId, String administratorSubject, String reason);

    Affiliation withdraw(UUID affiliationId, String administratorSubject, String reason);

    List<AffiliationAuditEntry> findAuditTrail(UUID affiliationId);
}
