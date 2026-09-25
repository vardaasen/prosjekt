package no.fagskolen.prosjekt.marketplace.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BusinessJpaRepository extends JpaRepository<BusinessJpaEntity, UUID> {

    Optional<BusinessJpaEntity> findByOrganisationNumber(String organisationNumber);
}
