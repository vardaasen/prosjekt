package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.applications.SellerRoleProvisioner;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplication;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationAuditEntry;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class JpaSellerApplications implements SellerApplications {

    private final SellerApplicationJpaRepository applications;
    private final SellerApplicationAuditJpaRepository auditEntries;
    private final SellerRoleProvisioner sellerRoleProvisioner;
    private final SellerAccountRegistry sellerAccountRegistry;

    JpaSellerApplications(
            SellerApplicationJpaRepository applications,
            SellerApplicationAuditJpaRepository auditEntries,
            SellerRoleProvisioner sellerRoleProvisioner,
            SellerAccountRegistry sellerAccountRegistry) {
        this.applications = applications;
        this.auditEntries = auditEntries;
        this.sellerRoleProvisioner = sellerRoleProvisioner;
        this.sellerAccountRegistry = sellerAccountRegistry;
    }

    @Override
    @Transactional
    public SellerApplication submit(String oidcSubject, Seller seller) {
        applications.findFirstByOidcSubjectOrderBySubmittedAtDesc(oidcSubject)
                .filter(application -> application.status() != SellerApplicationStatus.REJECTED)
                .ifPresent(application -> {
                    throw new IllegalStateException("A seller application is already pending or approved.");
                });
        var submittedAt = Instant.now();
        var application = applications.save(new SellerApplicationJpaEntity(
                UUID.randomUUID(),
                oidcSubject,
                seller.name(),
                seller.location(),
                submittedAt));
        auditEntries.save(new SellerApplicationAuditJpaEntity(
                application.id(),
                application.oidcSubject(),
                "SUBMITTED",
                submittedAt));
        return toDomain(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerApplication> findLatestFor(String oidcSubject) {
        return applications.findFirstByOidcSubjectOrderBySubmittedAtDesc(oidcSubject)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerApplication> findPending() {
        return applications.findByStatusOrderBySubmittedAtAsc(SellerApplicationStatus.PENDING).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerApplicationAuditEntry> findAuditTrail(UUID applicationId) {
        return auditEntries.findByApplicationIdOrderByOccurredAtAsc(applicationId).stream()
                .map(entry -> new SellerApplicationAuditEntry(
                        entry.applicationId(),
                        entry.actorSubject(),
                        entry.action(),
                        entry.occurredAt()))
                .toList();
    }

    @Override
    @Transactional
    public SellerApplication approve(UUID applicationId, String administratorSubject) {
        var application = requirePendingApplication(applicationId);
        sellerRoleProvisioner.grantSellerRole(application.oidcSubject());
        sellerAccountRegistry.activateSellerAccount(
                application.oidcSubject(),
                new Seller(application.sellerName(), true, application.sellerLocation()));
        var decidedAt = Instant.now();
        application.approve(administratorSubject, decidedAt);
        auditEntries.save(new SellerApplicationAuditJpaEntity(
                application.id(),
                administratorSubject,
                "APPROVED",
                decidedAt));
        return toDomain(application);
    }

    @Override
    @Transactional
    public SellerApplication reject(UUID applicationId, String administratorSubject, String reason) {
        var normalizedReason = requireReason(reason);
        var application = requirePendingApplication(applicationId);
        var decidedAt = Instant.now();
        application.reject(administratorSubject, normalizedReason, decidedAt);
        auditEntries.save(new SellerApplicationAuditJpaEntity(
                application.id(),
                administratorSubject,
                "REJECTED",
                decidedAt));
        return toDomain(application);
    }

    private SellerApplicationJpaEntity requirePendingApplication(UUID applicationId) {
        var application = applications.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Seller application was not found."));
        if (application.status() != SellerApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending seller applications can be decided.");
        }
        return application;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }
        var normalizedReason = reason.trim();
        if (normalizedReason.length() > 1000) {
            throw new IllegalArgumentException("A rejection reason must not exceed 1000 characters.");
        }
        return normalizedReason;
    }

    private SellerApplication toDomain(SellerApplicationJpaEntity application) {
        return new SellerApplication(
                application.id(),
                application.oidcSubject(),
                new Seller(application.sellerName(), application.status() == SellerApplicationStatus.APPROVED,
                        application.sellerLocation()),
                application.status(),
                application.submittedAt(),
                application.decidedAt(),
                application.decidedBy(),
                application.rejectionReason());
    }
}
