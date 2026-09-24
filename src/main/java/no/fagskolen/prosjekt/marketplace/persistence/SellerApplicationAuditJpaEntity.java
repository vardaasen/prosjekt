package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_application_audit")
class SellerApplicationAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Column(name = "actor_subject", nullable = false, updatable = false)
    private String actorSubject;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SellerApplicationAuditJpaEntity() {
    }

    SellerApplicationAuditJpaEntity(UUID applicationId, String actorSubject, String action, Instant occurredAt) {
        this.applicationId = applicationId;
        this.actorSubject = actorSubject;
        this.action = action;
        this.occurredAt = occurredAt;
    }

    UUID applicationId() {
        return applicationId;
    }

    String actorSubject() {
        return actorSubject;
    }

    String action() {
        return action;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
