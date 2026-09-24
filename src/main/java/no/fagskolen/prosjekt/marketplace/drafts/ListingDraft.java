package no.fagskolen.prosjekt.marketplace.drafts;

import no.fagskolen.prosjekt.marketplace.domain.EquipmentCategory;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;

import java.math.BigDecimal;
import java.util.Objects;

public record ListingDraft(
        String slug,
        String title,
        String location,
        ListingCondition condition,
        EquipmentCategory category,
        BigDecimal priceNok,
        String summary) {

    public ListingDraft {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(priceNok, "priceNok");
        Objects.requireNonNull(summary, "summary");

        slug = slug.trim();
        title = title.trim();
        location = location.trim();
        summary = summary.trim();

        if (slug.isEmpty() || title.isEmpty() || location.isEmpty() || summary.isEmpty()) {
            throw new IllegalArgumentException("draft fields must not be blank");
        }
        if (priceNok.signum() < 0) {
            throw new IllegalArgumentException("priceNok must not be negative");
        }
    }
}
