package no.fagskolen.prosjekt.marketplace.ui;

import no.fagskolen.prosjekt.marketplace.domain.OrganisationNumber;

import java.util.Optional;

/**
 * Feilmeldinger for organisasjonsnummer etter docs/design/feilmeldinger.md.
 */
final class OrganisationNumberMessages {

    private OrganisationNumberMessages() {
    }

    static Optional<String> errorFor(String value) {
        var digits = value == null ? "" : value.replace(" ", "");
        if (!digits.matches("\\d{9}")) {
            return Optional.of("Organisasjonsnummeret må være ni sifre.");
        }
        try {
            new OrganisationNumber(digits);
            return Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.of("Sjekk at organisasjonsnummeret er skrevet riktig.");
        }
    }
}
