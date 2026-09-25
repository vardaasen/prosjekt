package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationAuditEntry;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "affiliation_audit")
class AffiliationAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "affiliation_id", nullable = false, updatable = false)
    private UUID affiliationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false)
    private AffiliationAuditEntry.Action action;

    @Column(name = "actor_subject", nullable = false, updatable = false)
    private String actorSubject;

    @Column(name = "reason", updatable = false)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AffiliationAuditJpaEntity() {
    }

    AffiliationAuditJpaEntity(UUID affiliationId, AffiliationAuditEntry.Action action, String actorSubject,
            String reason, Instant occurredAt) {
        this.affiliationId = affiliationId;
        this.action = action;
        this.actorSubject = actorSubject;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    AffiliationAuditEntry toDomain() {
        return new AffiliationAuditEntry(affiliationId, action, actorSubject, reason, occurredAt);
    }
}
