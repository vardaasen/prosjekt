package no.fagskolen.prosjekt.marketplace.catalogue;

import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    @Test
    void excludesDraftListingsFromPublicCatalogueOperations() {
        var catalogue = new InMemoryPublishedListingCatalogue(List.of(
                listing("publisert-pumpe", ListingPublicationStatus.PUBLISHED),
                listing("notinspeksjon-70", ListingPublicationStatus.DRAFT)));

        assertThat(catalogue.findPublishedListings())
                .extracting("slug")
                .doesNotContain("notinspeksjon-70");
        assertThat(catalogue.findPublishedBySlug("notinspeksjon-70")).isEmpty();
    }

    private static Listing listing(String slug, ListingPublicationStatus publicationStatus) {
        return new Listing(
                slug,
                "Testutstyr",
                "Vestland",
                ListingCondition.GOOD,
                EquipmentCategory.PUMP,
                publicationStatus,
                BigDecimal.valueOf(20_000),
                new Seller("Selger AS", true, "Vestland"),
                LocalDate.of(2026, 1, 1),
                "Kort beskrivelse",
                List.of("CE"));
    }
}
