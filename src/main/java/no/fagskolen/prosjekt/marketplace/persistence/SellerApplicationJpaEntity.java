package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_application")
class SellerApplicationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "oidc_subject", nullable = false, updatable = false)
    private String oidcSubject;

    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "seller_location", nullable = false)
    private String sellerLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerApplicationStatus status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Version
    private long version;

    protected SellerApplicationJpaEntity() {
    }

    SellerApplicationJpaEntity(UUID id, String oidcSubject, String sellerName, String sellerLocation, Instant submittedAt) {
        this.id = id;
        this.oidcSubject = oidcSubject;
        this.sellerName = sellerName;
        this.sellerLocation = sellerLocation;
        this.status = SellerApplicationStatus.PENDING;
        this.submittedAt = submittedAt;
    }

    void approve(String administratorSubject, Instant decidedAt) {
        ensurePending();
        status = SellerApplicationStatus.APPROVED;
        this.decidedBy = administratorSubject;
        this.decidedAt = decidedAt;
        this.rejectionReason = null;
    }

    void reject(String administratorSubject, String reason, Instant decidedAt) {
        ensurePending();
        status = SellerApplicationStatus.REJECTED;
        this.decidedBy = administratorSubject;
        this.decidedAt = decidedAt;
        this.rejectionReason = reason;
    }

    private void ensurePending() {
        if (status != SellerApplicationStatus.PENDING) {
            throw new IllegalStateException("Only pending seller applications can be decided.");
        }
    }

    UUID id() {
        return id;
    }

    String oidcSubject() {
        return oidcSubject;
    }

    String sellerName() {
        return sellerName;
    }

    String sellerLocation() {
        return sellerLocation;
    }

    SellerApplicationStatus status() {
        return status;
    }

    Instant submittedAt() {
        return submittedAt;
    }

    Instant decidedAt() {
        return decidedAt;
    }

    String decidedBy() {
        return decidedBy;
    }

    String rejectionReason() {
        return rejectionReason;
    }
}
