package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

@Component
class ListingDataMapper {

    Listing toDomain(ListingJpaEntity entity) {
        return new Listing(
                entity.slug(),
                entity.title(),
                entity.location(),
                entity.condition(),
                entity.category(),
                entity.publicationStatus(),
                entity.priceNok(),
                new Seller(entity.sellerName(), entity.verifiedSeller(), entity.sellerLocation()),
                entity.publishedAt(),
                entity.summary(),
                entity.documentation().stream().toList());
    }

    ListingJpaEntity toEntity(Listing listing) {
        return new ListingJpaEntity(
                listing.slug(),
                listing.title(),
                listing.location(),
                listing.condition(),
                listing.category(),
                listing.publicationStatus(),
                listing.priceNok(),
                listing.seller().name(),
                listing.seller().verified(),
                listing.seller().location(),
                listing.publishedAt(),
                listing.summary(),
                new LinkedHashSet<>(listing.documentation()));
    }
}
