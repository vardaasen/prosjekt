package no.fagskolen.prosjekt.marketplace.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Virksomhet (CONTEXT.md): den juridiske enheten som er part i annonser, bud
 * og handler, identifisert med organisasjonsnummer.
 */
public record Business(UUID id, OrganisationNumber organisationNumber, String name, String location) {

    public Business {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(organisationNumber, "organisationNumber");
        Objects.requireNonNull(name, "name");
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Navnet på virksomheten må fylles ut.");
        }
        location = location == null ? "" : location.trim();
    }
}
