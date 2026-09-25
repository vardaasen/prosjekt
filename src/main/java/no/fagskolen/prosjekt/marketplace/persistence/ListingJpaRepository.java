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
            order by listing.publishedAt desc, listing.decidedAt desc nulls last, listing.id desc
            """)
    List<ListingJpaEntity> searchPublished(
            @Param("publicationStatus") ListingPublicationStatus publicationStatus,
            @Param("query") String query,
            @Param("location") String location,
            @Param("condition") ListingCondition condition);

    // Nyeste først; publishedAt er en dato, så godkjenningstidspunktet skiller
    // annonser publisert samme dag (funnet ved manuell test av #18).
    @Query("""
            select listing from ListingJpaEntity listing
            where listing.publicationStatus = :publicationStatus
            order by listing.publishedAt desc, listing.decidedAt desc nulls last, listing.id desc
            """)
    List<ListingJpaEntity> findAllByPublicationStatusOrderByPublishedAtDesc(
            @Param("publicationStatus") ListingPublicationStatus publicationStatus);

    // Bare en verifisert tilknytning gir innsyn i virksomhetens annonser (0009:
    // hvem som helst kan registrere et organisasjonsnummer).
    @Query("""
            select listing from ListingJpaEntity listing
            join fetch listing.business business
            where business.id in (
                    select affiliation.business.id from AffiliationJpaEntity affiliation
                    where affiliation.personSubject = :personSubject
                      and affiliation.status = no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus.VERIFIED)
            order by listing.id desc
            """)
    List<ListingJpaEntity> findForPerson(@Param("personSubject") String personSubject);

    @Query("""
            select listing from ListingJpaEntity listing join fetch listing.business
            where listing.publicationStatus = no.fagskolen.prosjekt.marketplace.domain.ListingPublicationStatus.SUBMITTED
            order by listing.submittedAt
            """)
    List<ListingJpaEntity> findAwaitingApproval();

    @Query("""
            select listing from ListingJpaEntity listing join fetch listing.business
            where listing.id = :id
            """)
    Optional<ListingJpaEntity> findWithBusiness(@Param("id") Long id);

    @Query("""
            select listing from ListingJpaEntity listing join fetch listing.business
            order by listing.id desc
            """)
    List<ListingJpaEntity> findAllOwnedByBusinesses();

    Optional<ListingJpaEntity> findBySlugAndPublicationStatus(
            String slug,
            ListingPublicationStatus publicationStatus);

    List<ListingJpaEntity> findAllBySellerAccountOidcSubjectAndPublicationStatusOrderByIdDesc(
            String oidcSubject,
            ListingPublicationStatus publicationStatus);

    Optional<ListingJpaEntity> findBySlugAndSellerAccountOidcSubjectAndPublicationStatus(
            String slug,
            String oidcSubject,
            ListingPublicationStatus publicationStatus);
}
