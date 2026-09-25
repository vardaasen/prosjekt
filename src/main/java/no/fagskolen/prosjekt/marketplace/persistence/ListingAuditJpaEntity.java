package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.fagskolen.prosjekt.marketplace.listings.ListingAuditEntry;

import java.time.Instant;

@Entity
@Table(name = "listing_audit")
class ListingAuditJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private long listingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false)
    private ListingAuditEntry.Action action;

    @Column(name = "actor_subject", nullable = false, updatable = false)
    private String actorSubject;

    @Column(name = "note", updatable = false)
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ListingAuditJpaEntity() {
    }

    ListingAuditJpaEntity(long listingId, ListingAuditEntry.Action action, String actorSubject, String note,
            Instant occurredAt) {
        this.listingId = listingId;
        this.action = action;
        this.actorSubject = actorSubject;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    ListingAuditEntry toDomain() {
        return new ListingAuditEntry(listingId, action, actorSubject, note, occurredAt);
    }
}
