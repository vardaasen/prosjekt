package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Annonsens livssyklus (userflow 12, domenemodellen): utkast sendes til
 * godkjenning av en verifisert tilknytning; administratoren godkjenner eller
 * ber om endring med tilbakemelding (0007).
 */
class ListingPublicationStatusTest {

    @Test
    void aDraftFromAVerifiedAffiliationCanBeSubmittedForApproval() {
        assertThat(ListingPublicationStatus.DRAFT.submit(AffiliationStatus.VERIFIED))
                .isEqualTo(ListingPublicationStatus.SUBMITTED);
    }

    @Test
    void anUnverifiedAffiliationCannotSubmitForApproval() {
        assertThatIllegalStateException()
                .isThrownBy(() -> ListingPublicationStatus.DRAFT.submit(AffiliationStatus.PENDING))
                .withMessage("Tilknytningen din til virksomheten må være verifisert før du kan sende annonsen til godkjenning.");
    }

    @Test
    void theAdministratorApprovesOrAsksForChanges() {
        assertThat(ListingPublicationStatus.SUBMITTED.approve()).isEqualTo(ListingPublicationStatus.PUBLISHED);
        assertThat(ListingPublicationStatus.SUBMITTED.returnForChanges("Beskriv tilstanden nærmere."))
                .isEqualTo(ListingPublicationStatus.DRAFT);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ListingPublicationStatus.SUBMITTED.returnForChanges(" "));
    }

    @Test
    void refusesTransitionsTheLifeCycleDoesNotAllow() {
        assertThatIllegalStateException()
                .isThrownBy(() -> ListingPublicationStatus.PUBLISHED.submit(AffiliationStatus.VERIFIED));
        assertThatIllegalStateException().isThrownBy(ListingPublicationStatus.DRAFT::approve);
        assertThatIllegalStateException()
                .isThrownBy(() -> ListingPublicationStatus.PUBLISHED.returnForChanges("Nei."));
    }

    @Test
    void draftsRequireAVerifiedAffiliation() {
        // Produkteier 2026-09-25: bare utkast laget med verifisert tilknytning havner
        // hos kollegaene i virksomheten.
        assertThat(ListingPublicationStatus.mayCreateDraft(AffiliationStatus.PENDING)).isFalse();
        assertThat(ListingPublicationStatus.mayCreateDraft(AffiliationStatus.VERIFIED)).isTrue();
        assertThat(ListingPublicationStatus.mayCreateDraft(AffiliationStatus.REJECTED)).isFalse();
        assertThat(ListingPublicationStatus.mayCreateDraft(AffiliationStatus.WITHDRAWN)).isFalse();
    }
}
