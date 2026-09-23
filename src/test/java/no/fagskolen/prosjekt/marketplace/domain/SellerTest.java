package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerTest {

    @Test
    void rejectsBlankSellerName() {
        assertThatThrownBy(() -> new Seller("   ", true, "Trøndelag"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void normalizesSellerNameAndKeepsVerification() {
        var seller = new Seller("  Fjord Drift AS  ", true, "  Trøndelag  ");

        assertThat(seller.name()).isEqualTo("Fjord Drift AS");
        assertThat(seller.location()).isEqualTo("Trøndelag");
        assertThat(seller.verified()).isTrue();
    }
}
