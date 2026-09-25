package no.fagskolen.prosjekt.marketplace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AffiliationJpaRepository extends JpaRepository<AffiliationJpaEntity, UUID> {

    @Query("""
            select a from AffiliationJpaEntity a join fetch a.business
            where a.personSubject = :personSubject
            order by a.registeredAt""")
    List<AffiliationJpaEntity> findForPerson(String personSubject);

    @Query("""
            select a from AffiliationJpaEntity a join fetch a.business
            where a.status = no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus.PENDING
            order by a.registeredAt""")
    List<AffiliationJpaEntity> findPending();

    @Query("""
            select a from AffiliationJpaEntity a join fetch a.business
            order by a.registeredAt desc""")
    List<AffiliationJpaEntity> findAllWithBusiness();

    @Query("""
            select a from AffiliationJpaEntity a join fetch a.business
            where a.id = :id""")
    Optional<AffiliationJpaEntity> findWithBusiness(UUID id);

    @Query("""
            select a from AffiliationJpaEntity a join fetch a.business b
            where a.personSubject = :personSubject and b.id = :businessId""")
    Optional<AffiliationJpaEntity> findForPersonAndBusiness(String personSubject, UUID businessId);
}
