package no.fagskolen.prosjekt.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tilknytning (CONTEXT.md): koblingen som lar en person handle på vegne av en
 * virksomhet. Personen lagres bare som Keycloak-subject (dataminimering, 0009).
 */
public record Affiliation(
        UUID id,
        String personSubject,
        Business business,
        AffiliationStatus status,
        Instant registeredAt) {

    public Affiliation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(personSubject, "personSubject");
        Objects.requireNonNull(business, "business");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(registeredAt, "registeredAt");
        personSubject = personSubject.trim();
        if (personSubject.isEmpty()) {
            throw new IllegalArgumentException("personSubject must not be blank");
        }
    }
}
