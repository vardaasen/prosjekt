package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.catalogue.PublishedListingCatalogue;
import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Primary
class JpaPublishedListingCatalogue implements PublishedListingCatalogue {

    private final ListingJpaRepository listings;
    private final ListingDataMapper mapper;

    JpaPublishedListingCatalogue(ListingJpaRepository listings, ListingDataMapper mapper) {
        this.listings = listings;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findPublishedListings() {
        return listings.findAllByPublicationStatusOrderByPublishedAtDesc(ListingPublicationStatus.PUBLISHED)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> searchPublishedListings(String query, String location, ListingCondition condition) {
        return listings.searchPublished(
                        ListingPublicationStatus.PUBLISHED,
                        normalize(query),
                        normalize(location),
                        condition)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findPublishedBySlug(String slug) {
        return listings.findBySlugAndPublicationStatus(slug, ListingPublicationStatus.PUBLISHED)
                .map(mapper::toDomain);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
