package no.fagskolen.prosjekt.marketplace.listings;

import java.time.Instant;
import java.util.Objects;

/**
 * Auditspor for en annonse: hvem som gjorde hva og når. Sporet er bare til å
 * legge til. {@code note} er tilbakemelding eller merknad.
 */
public record ListingAuditEntry(long listingId, Action action, String actorSubject, String note, Instant occurredAt) {

    public enum Action {
        CREATED,
        UPDATED,
        SUBMITTED,
        /** En verifisert kollega sendte inn et utkast fra en person som ikke lenger var verifisert, og bekreftet det. */
        SUBMITTED_FOR_UNVERIFIED_CREATOR,
        APPROVED,
        RETURNED_FOR_CHANGES
    }

    public ListingAuditEntry {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(actorSubject, "actorSubject");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
