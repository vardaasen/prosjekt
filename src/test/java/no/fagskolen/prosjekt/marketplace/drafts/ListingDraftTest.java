package no.fagskolen.prosjekt.marketplace.drafts;

import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingDraftTest {

    @Test
    void rejectsBlankDraftFields() {
        assertThatThrownBy(() -> new ListingDraft(
                "draft",
                "  ",
                "Trøndelag",
                ListingCondition.GOOD,
                EquipmentCategory.PUMP,
                BigDecimal.ZERO,
                "Beskrivelse"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("draft fields must not be blank");
    }
}
