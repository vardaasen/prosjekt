package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.Business;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
class JpaAffiliations implements Affiliations {

    private final BusinessJpaRepository businesses;
    private final AffiliationJpaRepository affiliations;
    private final Clock clock;

    JpaAffiliations(BusinessJpaRepository businesses, AffiliationJpaRepository affiliations) {
        this.businesses = businesses;
        this.affiliations = affiliations;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public Affiliation register(
            String personSubject, OrganisationNumber organisationNumber, String name, String location) {
        var now = clock.instant();
        var requested = new Business(UUID.randomUUID(), organisationNumber, name, location);
        var business = businesses.findByOrganisationNumber(organisationNumber.value())
                .orElseGet(() -> businesses.save(new BusinessJpaEntity(
                        requested.id(), organisationNumber.value(), requested.name(), requested.location(), now)));
        var affiliation = affiliations.findForPersonAndBusiness(personSubject.trim(), business.id())
                .orElseGet(() -> affiliations.save(
                        new AffiliationJpaEntity(UUID.randomUUID(), personSubject.trim(), business, now)));
        return toDomain(affiliation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Affiliation> findFor(String personSubject) {
        return affiliations.findForPerson(personSubject.trim()).stream().map(JpaAffiliations::toDomain).toList();
    }

    private static Affiliation toDomain(AffiliationJpaEntity entity) {
        var business = entity.business();
        return new Affiliation(
                entity.id(),
                entity.personSubject(),
                new Business(business.id(), new OrganisationNumber(business.organisationNumber()),
                        business.name(), business.location()),
                entity.status(),
                entity.registeredAt());
    }
}
