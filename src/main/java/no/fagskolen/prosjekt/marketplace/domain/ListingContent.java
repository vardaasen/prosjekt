package no.fagskolen.prosjekt.marketplace.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Det personen skriver i en annonse (userflow 12). Slug og eier settes av
 * markedsplassen, ikke av personen.
 */
public record ListingContent(
        String title,
        String location,
        ListingCondition condition,
        EquipmentCategory category,
        BigDecimal priceNok,
        String summary) {

    public ListingContent {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(priceNok, "priceNok");
        title = requireText(title, "tittel");
        location = requireText(location, "lokasjon");
        summary = requireText(summary, "beskrivelse");
        if (priceNok.signum() < 0) {
            throw new IllegalArgumentException("Prisen kan ikke være negativ.");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Skriv inn " + field + ".");
        }
        return value.trim();
    }
}
