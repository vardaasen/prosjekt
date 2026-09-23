package no.fagskolen.prosjekt.marketplace.catalogue;

import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPublishedListingCatalogueTest {

    private final PublishedListingCatalogue catalogue = new InMemoryPublishedListingCatalogue();

    @Test
    void providesThePublishedListingCatalogueSeam() {
        assertThat(catalogue.findPublishedListings()).hasSize(3);
        assertThat(catalogue.findPublishedBySlug("sentrifugalpumpe-450")).isPresent();
    }

    @Test
    void filtersListingsBySearchLocationAndCondition() {
        var results = catalogue.searchPublishedListings("pumpe", "Trøndelag", ListingCondition.GOOD);

        assertThat(results).extracting("slug").containsExactly("sentrifugalpumpe-450");
    }

    @Test
    void returnsAllListingsForBlankFilters() {
        assertThat(catalogue.searchPublishedListings("", "", null)).hasSize(3);
    }
}
