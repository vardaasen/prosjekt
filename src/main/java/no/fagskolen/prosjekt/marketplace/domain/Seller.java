package no.fagskolen.prosjekt.marketplace.domain;

import java.util.Objects;

public record Seller(String name, boolean verified, String location) {

    public Seller {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        name = name.trim();
        location = location == null ? "" : location.trim();
    }

    public Seller(String name, boolean verified) {
        this(name, verified, "");
    }
}
