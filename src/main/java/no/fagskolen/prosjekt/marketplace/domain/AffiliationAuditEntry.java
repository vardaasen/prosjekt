package no.fagskolen.prosjekt.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Auditspor for en tilknytning (0006: administrator-subject, beslutning og
 * tidspunkt). Sporet er bare til å legge til; det endres eller slettes aldri.
 */
public record AffiliationAuditEntry(
        UUID affiliationId,
        Action action,
        String actorSubject,
        String reason,
        Instant occurredAt) {

    public enum Action {
        REGISTERED,
        VERIFIED,
        REJECTED,
        WITHDRAWN
    }

    public AffiliationAuditEntry {
        Objects.requireNonNull(affiliationId, "affiliationId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorSubject, "actorSubject");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
