package no.fagskolen.prosjekt.marketplace.listings;

import no.fagskolen.prosjekt.marketplace.domain.ListingContent;

import java.util.List;
import java.util.UUID;

/**
 * Virksomhetens annonser fra utkast til publisert (userflow 12, 0007). En person
 * handler gjennom sin tilknytning til virksomheten; administratoren godkjenner.
 * Brudd på livssyklus eller tilgang gir {@link IllegalStateException}.
 */
public interface BusinessListings {

    /** Krever verifisert tilknytning til virksomheten. */
    BusinessListing createDraft(String personSubject, UUID businessId, ListingContent content);

    /** Annonser til alle virksomheter personen har verifisert tilknytning til. */
    List<BusinessListing> findForPerson(String personSubject);

    /** Bare utkast kan endres, av en person med verifisert tilknytning. */
    BusinessListing updateDraft(String personSubject, long listingId, ListingContent content);

    /**
     * Krever verifisert tilknytning til annonsens virksomhet. Er utkastet laget av en
     * annen person som ikke lenger er verifisert, må innsenderen skrive en merknad om
     * hva som er kontrollert ({@code reviewNote}); merknaden står i auditsporet.
     */
    BusinessListing submitForApproval(String personSubject, long listingId, String reviewNote);

    List<BusinessListing> findAwaitingApproval();

    /** Alle annonser for administratorens oversikt, nyeste først. */
    List<BusinessListing> findAll();

    List<ListingAuditEntry> findAuditTrail(long listingId);

    BusinessListing approve(long listingId, String administratorSubject);

    BusinessListing returnForChanges(long listingId, String administratorSubject, String feedback);
}
