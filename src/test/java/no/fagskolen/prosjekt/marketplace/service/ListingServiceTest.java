package no.fagskolen.prosjekt.marketplace.service;

import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListingServiceTest {

    private final ListingService listingService = new ListingService();

    @Test
    void filtersListingsBySearchLocationAndCondition() {
        var results = listingService.searchPublicListings("pumpe", "Trøndelag", ListingCondition.GOOD);

        assertThat(results).extracting("slug").containsExactly("sentrifugalpumpe-450");
    }

    @Test
    void returnsAllListingsForBlankFilters() {
        assertThat(listingService.searchPublicListings("", "", null)).hasSize(3);
    }
}
