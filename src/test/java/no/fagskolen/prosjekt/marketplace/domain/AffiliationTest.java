package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tilknytningens livssyklus (domenemodellen, userflow 11): venter kan
 * verifiseres eller avvises med begrunnelse; verifisert kan trekkes tilbake
 * med begrunnelse; avvist og trukket tilbake er endelige.
 */
class AffiliationTest {

    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");

    @Test
    void aPendingAffiliationCanBeVerified() {
        var verified = pending().verify("admin-subject", NOW);

        assertThat(verified.status()).isEqualTo(AffiliationStatus.VERIFIED);
        assertThat(verified.decidedBy()).isEqualTo("admin-subject");
        assertThat(verified.decidedAt()).isEqualTo(NOW);
    }

    @Test
    void aPendingAffiliationCanBeRejectedWithAReason() {
        var rejected = pending().reject("admin-subject", "Personen er ikke knyttet til virksomheten.", NOW);

        assertThat(rejected.status()).isEqualTo(AffiliationStatus.REJECTED);
        assertThat(rejected.reason()).isEqualTo("Personen er ikke knyttet til virksomheten.");
    }

    @Test
    void aVerifiedAffiliationCanBeWithdrawnWithAReason() {
        var withdrawn = pending().verify("admin-subject", NOW)
                .withdraw("other-admin", "Personen har sluttet i virksomheten.", NOW.plusSeconds(60));

        assertThat(withdrawn.status()).isEqualTo(AffiliationStatus.WITHDRAWN);
        assertThat(withdrawn.decidedBy()).isEqualTo("other-admin");
    }

    @Test
    void rejectingOrWithdrawingRequiresAReason() {
        assertThatIllegalArgumentException().isThrownBy(() -> pending().reject("admin-subject", " ", NOW));
        assertThatIllegalArgumentException().isThrownBy(() ->
                pending().verify("admin-subject", NOW).withdraw("admin-subject", "", NOW));
    }

    @Test
    void refusesTransitionsTheLifeCycleDoesNotAllow() {
        var verified = pending().verify("admin-subject", NOW);
        var rejected = pending().reject("admin-subject", "Ukjent person.", NOW);

        assertThatIllegalStateException().isThrownBy(() -> verified.verify("admin-subject", NOW));
        assertThatIllegalStateException().isThrownBy(() -> pending().withdraw("admin-subject", "Grunn.", NOW));
        assertThatIllegalStateException().isThrownBy(() -> rejected.verify("admin-subject", NOW));
        assertThatIllegalStateException().isThrownBy(() -> verified.reject("admin-subject", "Grunn.", NOW));
    }

    private static Affiliation pending() {
        var business = new Business(UUID.randomUUID(), new OrganisationNumber("974760673"), "Fjord Drift AS", "Bergen");
        return Affiliation.registered(UUID.randomUUID(), "person-subject", business, NOW.minusSeconds(3600));
    }
}
