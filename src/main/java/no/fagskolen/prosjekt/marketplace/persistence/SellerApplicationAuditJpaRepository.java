package no.fagskolen.prosjekt.marketplace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SellerApplicationAuditJpaRepository extends JpaRepository<SellerApplicationAuditJpaEntity, Long> {

    List<SellerApplicationAuditJpaEntity> findByApplicationIdOrderByOccurredAtAsc(UUID applicationId);
}
