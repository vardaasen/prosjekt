package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.catalogue.PublishedListingCatalogue;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
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
class JpaPublishedListingCatalogueTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private PublishedListingCatalogue catalogue;

    @Test
    void readsTheSeededPublishedCatalogue() {
        assertThat(catalogue.findPublishedListings())
                .extracting("slug")
                .containsExactly(
                        "sentrifugalpumpe-450",
                        "notpose-160",
                        "forflate-24");
    }

    @Test
    void searchesWithDatabaseBackedFilters() {
        assertThat(catalogue.searchPublishedListings("pumpe", "Trøndelag", ListingCondition.GOOD))
                .extracting("slug")
                .containsExactly("sentrifugalpumpe-450");
    }
}
