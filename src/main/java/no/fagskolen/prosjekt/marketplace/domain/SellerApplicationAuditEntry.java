package no.fagskolen.prosjekt.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SellerApplicationAuditEntry(
        UUID applicationId,
        String actorSubject,
        String action,
        Instant occurredAt) {

    public SellerApplicationAuditEntry {
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(actorSubject, "actorSubject");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
