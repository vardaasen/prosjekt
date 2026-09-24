package no.fagskolen.prosjekt.marketplace.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SellerApplication(
        UUID id,
        String oidcSubject,
        Seller seller,
        SellerApplicationStatus status,
        Instant submittedAt,
        Instant decidedAt,
        String decidedBy,
        String rejectionReason) {

    public SellerApplication {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(oidcSubject, "oidcSubject");
        Objects.requireNonNull(seller, "seller");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(submittedAt, "submittedAt");

        oidcSubject = oidcSubject.trim();
        if (oidcSubject.isEmpty()) {
            throw new IllegalArgumentException("oidcSubject must not be blank");
        }
    }
}
