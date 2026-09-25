package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ListingContentTest {

    @Test
    void trimsTheTextAndKeepsTheValues() {
        var content = new ListingContent(" Sentrifugalpumpe 450 ", " Bergen ", ListingCondition.GOOD,
                EquipmentCategory.OTHER, new BigDecimal("42000"), " Normal slitasje. ");

        assertThat(content.title()).isEqualTo("Sentrifugalpumpe 450");
        assertThat(content.location()).isEqualTo("Bergen");
        assertThat(content.summary()).isEqualTo("Normal slitasje.");
    }

    @Test
    void refusesBlankRequiredTextAndNegativePrices() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ListingContent(" ", "Bergen",
                ListingCondition.GOOD, EquipmentCategory.OTHER, BigDecimal.ONE, "Tekst."));
        assertThatIllegalArgumentException().isThrownBy(() -> new ListingContent("Pumpe", "Bergen",
                ListingCondition.GOOD, EquipmentCategory.OTHER, new BigDecimal("-1"), "Tekst."));
    }

    @Test
    void generatesAReadableSlugFromTheTitle() {
        assertThat(Slugs.fromTitle("Fôrflåte 24 m – med silo!", "k3x9"))
                .isEqualTo("forflate-24-m-med-silo-k3x9");
        assertThat(Slugs.fromTitle("Ærlig Ørret-merd", "a1b2")).isEqualTo("aerlig-orret-merd-a1b2");
    }
}
