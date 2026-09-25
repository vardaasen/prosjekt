package no.fagskolen.prosjekt.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tilknytning (CONTEXT.md): koblingen som lar en person handle på vegne av en
 * virksomhet. Personen lagres bare som Keycloak-subject (dataminimering, 0009).
 * Livssyklusen følger domenemodellen og userflow 11: venter kan verifiseres
 * eller avvises med begrunnelse; verifisert kan trekkes tilbake med
 * begrunnelse. Avvist og trukket tilbake er endelige.
 */
public record Affiliation(
        UUID id,
        String personSubject,
        Business business,
        AffiliationStatus status,
        Instant registeredAt,
        String decidedBy,
        Instant decidedAt,
        String reason) {

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

    public static Affiliation registered(UUID id, String personSubject, Business business, Instant registeredAt) {
        return new Affiliation(id, personSubject, business, AffiliationStatus.PENDING, registeredAt, null, null, null);
    }

    public Affiliation verify(String administratorSubject, Instant at) {
        requireStatus(AffiliationStatus.PENDING, "Bare en tilknytning som venter, kan verifiseres.");
        return decided(AffiliationStatus.VERIFIED, administratorSubject, at, null);
    }

    public Affiliation reject(String administratorSubject, String reason, Instant at) {
        requireStatus(AffiliationStatus.PENDING, "Bare en tilknytning som venter, kan avvises.");
        return decided(AffiliationStatus.REJECTED, administratorSubject, at, requireReason(reason));
    }

    public Affiliation withdraw(String administratorSubject, String reason, Instant at) {
        requireStatus(AffiliationStatus.VERIFIED, "Bare en verifisert tilknytning kan trekkes tilbake.");
        return decided(AffiliationStatus.WITHDRAWN, administratorSubject, at, requireReason(reason));
    }

    private void requireStatus(AffiliationStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Skriv inn en begrunnelse.");
        }
        return reason.trim();
    }

    private Affiliation decided(AffiliationStatus newStatus, String administratorSubject, Instant at, String newReason) {
        Objects.requireNonNull(administratorSubject, "administratorSubject");
        Objects.requireNonNull(at, "at");
        return new Affiliation(id, personSubject, business, newStatus, registeredAt, administratorSubject, at, newReason);
    }
}
