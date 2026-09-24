package no.fagskolen.prosjekt.marketplace.domain;

import java.util.Objects;

public record SellerAccount(String oidcSubject, Seller seller) {

    public SellerAccount {
        Objects.requireNonNull(oidcSubject, "oidcSubject");
        Objects.requireNonNull(seller, "seller");

        oidcSubject = oidcSubject.trim();
        if (oidcSubject.isEmpty()) {
            throw new IllegalArgumentException("oidcSubject must not be blank");
        }
    }
}
