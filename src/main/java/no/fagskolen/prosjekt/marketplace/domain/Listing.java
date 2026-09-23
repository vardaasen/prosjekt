package no.fagskolen.prosjekt.marketplace.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record Listing(
        String slug,
        String title,
        String location,
        ListingCondition condition,
        EquipmentCategory category,
        ListingPublicationStatus publicationStatus,
        BigDecimal priceNok,
        Seller seller,
        LocalDate publishedAt,
        String summary,
        List<String> documentation) {

    public Listing(String slug,
                   String title,
                   String location,
                   ListingCondition condition,
                   BigDecimal priceNok,
                   String sellerName,
                   boolean verifiedSeller,
                   LocalDate publishedAt,
                   String summary,
                   List<String> documentation) {
        this(slug,
                title,
                location,
                condition,
                EquipmentCategory.OTHER,
                ListingPublicationStatus.PUBLISHED,
                priceNok,
                new Seller(sellerName, verifiedSeller),
                publishedAt,
                summary,
                documentation);
    }

    public Listing {
        Objects.requireNonNull(slug, "slug");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(publicationStatus, "publicationStatus");
        Objects.requireNonNull(priceNok, "priceNok");
        Objects.requireNonNull(seller, "seller");
        Objects.requireNonNull(publishedAt, "publishedAt");
        Objects.requireNonNull(summary, "summary");
        documentation = List.copyOf(documentation);

        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (priceNok.signum() < 0) {
            throw new IllegalArgumentException("priceNok must not be negative");
        }
    }

    public String sellerName() {
        return seller.name();
    }

    public boolean verifiedSeller() {
        return seller.verified();
    }
}
