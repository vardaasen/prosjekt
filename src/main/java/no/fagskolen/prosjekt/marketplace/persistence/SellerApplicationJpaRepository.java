package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SellerApplicationJpaRepository extends JpaRepository<SellerApplicationJpaEntity, UUID> {

    Optional<SellerApplicationJpaEntity> findFirstByOidcSubjectOrderBySubmittedAtDesc(String oidcSubject);

    List<SellerApplicationJpaEntity> findByStatusOrderBySubmittedAtAsc(SellerApplicationStatus status);
}
