package no.fagskolen.prosjekt.marketplace.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business")
class BusinessJpaEntity {

    @Id
    private UUID id;

    @Column(name = "organisation_number", nullable = false, updatable = false)
    private String organisationNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    protected BusinessJpaEntity() {
    }

    BusinessJpaEntity(UUID id, String organisationNumber, String name, String location, Instant registeredAt) {
        this.id = id;
        this.organisationNumber = organisationNumber;
        this.name = name;
        this.location = location;
        this.registeredAt = registeredAt;
    }

    UUID id() {
        return id;
    }

    String organisationNumber() {
        return organisationNumber;
    }

    String name() {
        return name;
    }

    String location() {
        return location;
    }
}
