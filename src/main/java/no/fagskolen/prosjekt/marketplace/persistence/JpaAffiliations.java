package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.domain.Affiliation;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationAuditEntry;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;
import no.fagskolen.prosjekt.marketplace.domain.Business;
import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

@Service
class JpaAffiliations implements Affiliations {

    private final BusinessJpaRepository businesses;
    private final AffiliationJpaRepository affiliations;
    private final AffiliationAuditJpaRepository auditTrail;
    private final Clock clock;

    JpaAffiliations(
            BusinessJpaRepository businesses,
            AffiliationJpaRepository affiliations,
            AffiliationAuditJpaRepository auditTrail) {
        this.businesses = businesses;
        this.affiliations = affiliations;
        this.auditTrail = auditTrail;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public Affiliation register(
            String personSubject, OrganisationNumber organisationNumber, String name, String location) {
        var now = clock.instant();
        var subject = personSubject.trim();
        // En eksisterende virksomhet gjenbrukes uendret; navnet valideres bare når den
        // opprettes (funnet i #19: en kollega uten navn ble avvist).
        var business = businesses.findByOrganisationNumber(organisationNumber.value())
                .orElseGet(() -> {
                    var requested = new Business(UUID.randomUUID(), organisationNumber, name, location);
                    return businesses.save(new BusinessJpaEntity(
                            requested.id(), organisationNumber.value(), requested.name(), requested.location(), now));
                });
        var affiliation = affiliations.findForPersonAndBusiness(subject, business.id())
                .orElseGet(() -> {
                    var created = affiliations.save(
                            new AffiliationJpaEntity(UUID.randomUUID(), subject, business, now));
                    auditTrail.save(new AffiliationAuditJpaEntity(
                            created.id(), AffiliationAuditEntry.Action.REGISTERED, subject, null, now));
                    return created;
                });
        return toDomain(affiliation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Affiliation> findFor(String personSubject) {
        return affiliations.findForPerson(personSubject.trim()).stream().map(JpaAffiliations::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Affiliation> findPending() {
        return affiliations.findPending().stream().map(JpaAffiliations::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Affiliation> findAll() {
        return affiliations.findAllWithBusiness().stream().map(JpaAffiliations::toDomain).toList();
    }

    @Override
    @Transactional
    public Affiliation verify(UUID affiliationId, String administratorSubject) {
        return decide(affiliationId, administratorSubject, AffiliationAuditEntry.Action.VERIFIED,
                (affiliation, at) -> affiliation.verify(administratorSubject, at));
    }

    @Override
    @Transactional
    public Affiliation reject(UUID affiliationId, String administratorSubject, String reason) {
        return decide(affiliationId, administratorSubject, AffiliationAuditEntry.Action.REJECTED,
                (affiliation, at) -> affiliation.reject(administratorSubject, reason, at));
    }

    @Override
    @Transactional
    public Affiliation withdraw(UUID affiliationId, String administratorSubject, String reason) {
        return decide(affiliationId, administratorSubject, AffiliationAuditEntry.Action.WITHDRAWN,
                (affiliation, at) -> affiliation.withdraw(administratorSubject, reason, at));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffiliationAuditEntry> findAuditTrail(UUID affiliationId) {
        return auditTrail.findByAffiliationIdOrderByOccurredAtAscIdAsc(affiliationId).stream()
                .map(AffiliationAuditJpaEntity::toDomain)
                .toList();
    }

    private Affiliation decide(
            UUID affiliationId,
            String administratorSubject,
            AffiliationAuditEntry.Action action,
            BiFunction<Affiliation, Instant, Affiliation> transition) {
        var entity = affiliations.findWithBusiness(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke tilknytningen."));
        var decided = transition.apply(toDomain(entity), clock.instant());
        entity.recordDecision(decided.status(), decided.decidedBy(), decided.decidedAt(), decided.reason());
        try {
            affiliations.saveAndFlush(entity);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new IllegalStateException("Tilknytningen ble nettopp avgjort av en annen administrator.", exception);
        }
        auditTrail.save(new AffiliationAuditJpaEntity(
                affiliationId, action, administratorSubject, decided.reason(), decided.decidedAt()));
        return decided;
    }

    private static Affiliation toDomain(AffiliationJpaEntity entity) {
        var business = entity.business();
        return new Affiliation(
                entity.id(),
                entity.personSubject(),
                new Business(business.id(), new OrganisationNumber(business.organisationNumber()),
                        business.name(), business.location()),
                entity.status(),
                entity.registeredAt(),
                entity.decidedBy(),
                entity.decidedAt(),
                entity.reason());
    }
}
