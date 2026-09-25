package no.fagskolen.prosjekt.marketplace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AffiliationAuditJpaRepository extends JpaRepository<AffiliationAuditJpaEntity, Long> {

    List<AffiliationAuditJpaEntity> findByAffiliationIdOrderByOccurredAtAscIdAsc(UUID affiliationId);
}
