package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingTest {

    @Test
    void rejectsNegativePrice() {
        assertThatThrownBy(() -> listing(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("priceNok must not be negative");
    }

    @Test
    void protectsDocumentationFromMutation() {
        var listing = listing(BigDecimal.TEN);

        assertThatThrownBy(() -> listing.documentation().add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(listing.documentation()).containsExactly("CE");
    }

    @Test
    void usesTypedSellerAndCategoryForMarketplaceDomain() {
        var seller = new Seller("Fjord Drift AS", true, "Trøndelag");
        var listing = new Listing(
                "test",
                "Testutstyr",
                "Vestland",
                ListingCondition.GOOD,
                EquipmentCategory.PUMP,
                ListingPublicationStatus.PUBLISHED,
                BigDecimal.valueOf(20_000),
                seller,
                LocalDate.of(2026, 1, 1),
                "Kort beskrivelse",
                List.of("CE"));

        assertThat(listing.category()).isEqualTo(EquipmentCategory.PUMP);
        assertThat(listing.publicationStatus()).isEqualTo(ListingPublicationStatus.PUBLISHED);
        assertThat(listing.sellerName()).isEqualTo("Fjord Drift AS");
        assertThat(listing.verifiedSeller()).isTrue();
    }

    private static Listing listing(BigDecimal price) {
        return new Listing(
                "test",
                "Testutstyr",
                "Vestland",
                ListingCondition.GOOD,
                price,
                "Selger AS",
                true,
                LocalDate.of(2026, 1, 1),
                "Kort beskrivelse",
                List.of("CE"));
    }
}
