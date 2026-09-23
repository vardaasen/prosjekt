package no.fagskolen.prosjekt.marketplace.catalogue;

import no.fagskolen.prosjekt.marketplace.domain.Listing;
import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;

import java.util.List;
import java.util.Optional;

public interface PublishedListingCatalogue {

    List<Listing> findPublishedListings();

    List<Listing> searchPublishedListings(String query, String location, ListingCondition condition);

    Optional<Listing> findPublishedBySlug(String slug);
}
