package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.domain.ListingCondition;
import no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ListingJpaRepository extends JpaRepository<ListingJpaEntity, Long> {

    @Query("""
            select listing
            from ListingJpaEntity listing
            where listing.publicationStatus = :publicationStatus
              and (:query = '' or lower(listing.title) like concat('%', :query, '%')
                   or lower(listing.summary) like concat('%', :query, '%')
                   or lower(listing.sellerName) like concat('%', :query, '%'))
              and (:location = '' or lower(listing.location) = :location)
              and (:condition is null or listing.condition = :condition)
            order by listing.publishedAt desc
            """)
    List<ListingJpaEntity> searchPublished(
            @Param("publicationStatus") ListingPublicationStatus publicationStatus,
            @Param("query") String query,
            @Param("location") String location,
            @Param("condition") ListingCondition condition);

    List<ListingJpaEntity> findAllByPublicationStatusOrderByPublishedAtDesc(
            ListingPublicationStatus publicationStatus);

    Optional<ListingJpaEntity> findBySlugAndPublicationStatus(
            String slug,
            ListingPublicationStatus publicationStatus);

    List<ListingJpaEntity> findAllBySellerAccountOidcSubjectAndPublicationStatusOrderByIdDesc(
            String oidcSubject,
            ListingPublicationStatus publicationStatus);
}
