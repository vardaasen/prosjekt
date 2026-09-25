package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JpaAffiliationsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private Affiliations affiliations;

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
}
