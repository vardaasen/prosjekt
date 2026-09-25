package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationAuditEntry;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@SpringBootTest
@Testcontainers
class JpaAffiliationsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private Affiliations affiliations;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registeringABusinessCreatesAPendingAffiliationForThePerson() {
        var affiliation = affiliations.register(
                "person-registers", new OrganisationNumber("974760673"), "Fjord Drift AS", "Bergen");

        assertThat(affiliation.status()).isEqualTo(AffiliationStatus.PENDING);
        assertThat(affiliation.personSubject()).isEqualTo("person-registers");
        assertThat(affiliation.business().organisationNumber().value()).isEqualTo("974760673");
        assertThat(affiliation.business().name()).isEqualTo("Fjord Drift AS");
        assertThat(affiliations.findFor("person-registers")).containsExactly(affiliation);
    }

    @Test
    void aSecondPersonJoinsTheExistingBusinessWithoutChangingIt() {
        var first = affiliations.register(
                "first-person", new OrganisationNumber("923609016"), "Havbruk Nord AS", "Tromsø");
        var second = affiliations.register(
                "second-person", new OrganisationNumber("923 609 016"), "Et annet navn", "Et annet sted");

        assertThat(second.business()).isEqualTo(first.business());
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.status()).isEqualTo(AffiliationStatus.PENDING);
    }

    @Test
    void registeringTheSameBusinessTwiceGivesTheSameAffiliation() {
        var first = affiliations.register(
                "repeating-person", new OrganisationNumber("986105174"), "Merd Service AS", "Ålesund");
        var second = affiliations.register(
                "repeating-person", new OrganisationNumber("986105174"), "Merd Service AS", "Ålesund");

        assertThat(second).isEqualTo(first);
        assertThat(affiliations.findFor("repeating-person")).hasSize(1);
    }

    @Test
    void aPersonOnlySeesTheirOwnAffiliations() {
        affiliations.register("owner-person", new OrganisationNumber("914778271"), "Fôr og Flåte AS", "Molde");

        assertThat(affiliations.findFor("someone-else")).isEmpty();
    }

    @Test
    void anAdministratorVerifiesAPendingAffiliationAndTheAuditTrailRecordsIt() {
        var registered = affiliations.register("verified-person", organisationNumber("91234560"), "Kyst AS", "Bodø");

        assertThat(affiliations.findPending()).extracting(Affiliation::id).contains(registered.id());

        var verified = affiliations.verify(registered.id(), "admin-one");

        assertThat(verified.status()).isEqualTo(AffiliationStatus.VERIFIED);
        assertThat(verified.decidedBy()).isEqualTo("admin-one");
        assertThat(verified.decidedAt()).isNotNull();
        assertThat(affiliations.findPending()).extracting(Affiliation::id).doesNotContain(registered.id());
        assertThat(affiliations.findAll()).extracting(Affiliation::id).contains(registered.id());
        assertThat(affiliations.findAuditTrail(registered.id()))
                .extracting(AffiliationAuditEntry::action, AffiliationAuditEntry::actorSubject)
                .containsExactly(
                        tuple(AffiliationAuditEntry.Action.REGISTERED, "verified-person"),
                        tuple(AffiliationAuditEntry.Action.VERIFIED, "admin-one"));
    }

    @Test
    void rejectingKeepsTheReasonAndWithdrawingNeedsAVerifiedAffiliation() {
        var rejected = affiliations.reject(
                affiliations.register("rejected-person", organisationNumber("91234561"), "Laks AS", "Hitra").id(),
                "admin-one", "Ingen kobling til virksomheten.");
        var pending = affiliations.register("waiting-person", organisationNumber("91234562"), "Rogn AS", "Frøya");

        assertThat(rejected.status()).isEqualTo(AffiliationStatus.REJECTED);
        assertThat(rejected.reason()).isEqualTo("Ingen kobling til virksomheten.");
        assertThatIllegalStateException()
                .isThrownBy(() -> affiliations.withdraw(pending.id(), "admin-one", "Grunn."));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> affiliations.reject(pending.id(), "admin-one", " "));
        assertThat(affiliations.findAll()).filteredOn(affiliation -> affiliation.id().equals(pending.id()))
                .extracting(Affiliation::status).containsExactly(AffiliationStatus.PENDING);
    }

    @Test
    void aSecondAdministratorCannotDecideAnAffiliationThatIsAlreadyDecided() {
        var registered = affiliations.register("contested-person", organisationNumber("91234563"), "Merd AS", "Sula");
        affiliations.verify(registered.id(), "admin-one");

        assertThatIllegalStateException()
                .isThrownBy(() -> affiliations.reject(registered.id(), "admin-two", "For sent."));
        assertThat(affiliations.findAuditTrail(registered.id())).hasSize(2);
    }

    @Test
    void theAuditTrailCannotBeChangedOrDeleted() {
        var registered = affiliations.register("audited-person", organisationNumber("91234564"), "Nøter AS", "Vikna");

        assertThatThrownBy(() -> jdbc.update("update affiliation_audit set actor_subject = 'someone' where affiliation_id = ?",
                registered.id())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("delete from affiliation_audit where affiliation_id = ?",
                registered.id())).isInstanceOf(DataAccessException.class);
    }

    /** Gyldig organisasjonsnummer fra åtte sifre (kontrollsiffer etter modulus 11). */
    private static OrganisationNumber organisationNumber(String firstEightDigits) {
        int[] weights = {3, 2, 7, 6, 5, 4, 3, 2};
        for (var candidate = Integer.parseInt(firstEightDigits); ; candidate++) {
            var digits = String.format("%08d", candidate);
            var sum = 0;
            for (var i = 0; i < 8; i++) {
                sum += weights[i] * Character.digit(digits.charAt(i), 10);
            }
            var check = sum % 11 == 0 ? 0 : 11 - sum % 11;
            if (check != 10) {
                return new OrganisationNumber(digits + check);
            }
        }
    }
}
