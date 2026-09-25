package no.fagskolen.prosjekt.marketplace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ListingAuditJpaRepository extends JpaRepository<ListingAuditJpaEntity, Long> {

    List<ListingAuditJpaEntity> findByListingIdOrderByOccurredAtAscIdAsc(long listingId);
}
