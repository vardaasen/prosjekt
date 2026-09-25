package no.fagskolen.prosjekt.marketplace.domain;

/**
 * Publiseringsstatus (CONTEXT.md) med annonsens livssyklus (userflow 12,
 * domenemodellen, 0007): utkast → til godkjenning → publisert → arkivert.
 * Bare en verifisert tilknytning kan sende til godkjenning; administratoren
 * godkjenner eller ber om endring med tilbakemelding.
 */
public enum ListingPublicationStatus {
    DRAFT,
    SUBMITTED,
    PUBLISHED,
    ARCHIVED;

    /**
     * Utkast krever verifisert tilknytning (produkteier 2026-09-25, tillegg til 0007):
     * bare utkast laget av verifiserte personer havner hos kollegaene i virksomheten.
     */
    public static boolean mayCreateDraft(AffiliationStatus affiliationStatus) {
        return affiliationStatus == AffiliationStatus.VERIFIED;
    }

    public ListingPublicationStatus submit(AffiliationStatus affiliationStatus) {
        require(DRAFT, "Bare et utkast kan sendes til godkjenning.");
        if (affiliationStatus != AffiliationStatus.VERIFIED) {
            throw new IllegalStateException(
                    "Tilknytningen din til virksomheten må være verifisert før du kan sende annonsen til godkjenning.");
        }
        return SUBMITTED;
    }

    /** Bare et utkast kan endres; innsendte og publiserte annonser er låst. */
    public ListingPublicationStatus edit() {
        require(DRAFT, "Bare et utkast kan endres.");
        return DRAFT;
    }

    public ListingPublicationStatus approve() {
        require(SUBMITTED, "Bare en annonse som venter på godkjenning, kan godkjennes.");
        return PUBLISHED;
    }

    public ListingPublicationStatus returnForChanges(String feedback) {
        require(SUBMITTED, "Bare en annonse som venter på godkjenning, kan sendes tilbake.");
        if (feedback == null || feedback.isBlank()) {
            throw new IllegalArgumentException("Skriv inn hva som må endres.");
        }
        return DRAFT;
    }

    private void require(ListingPublicationStatus expected, String message) {
        if (this != expected) {
            throw new IllegalStateException(message);
        }
    }
}
