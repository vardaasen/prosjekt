package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.fagskolen.prosjekt.marketplace.domain.AffiliationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "affiliation")
class AffiliationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "person_subject", nullable = false, updatable = false)
    private String personSubject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false, updatable = false)
    private BusinessJpaEntity business;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AffiliationStatus status;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected AffiliationJpaEntity() {
    }

    AffiliationJpaEntity(UUID id, String personSubject, BusinessJpaEntity business, Instant registeredAt) {
        this.id = id;
        this.personSubject = personSubject;
        this.business = business;
        this.status = AffiliationStatus.PENDING;
        this.registeredAt = registeredAt;
    }

    UUID id() {
        return id;
    }

    String personSubject() {
        return personSubject;
    }

    BusinessJpaEntity business() {
        return business;
    }

    AffiliationStatus status() {
        return status;
    }

    Instant registeredAt() {
        return registeredAt;
    }
}
