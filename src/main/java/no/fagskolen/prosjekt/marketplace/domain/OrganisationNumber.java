package no.fagskolen.prosjekt.marketplace.domain;

import java.util.Objects;

/**
 * Organisasjonsnummeret som identifiserer en virksomhet (CONTEXT.md): ni
 * sifre der det siste er et kontrollsiffer etter modulus 11.
 */
public record OrganisationNumber(String value) {

    private static final int[] WEIGHTS = {3, 2, 7, 6, 5, 4, 3, 2};

    public OrganisationNumber {
        Objects.requireNonNull(value, "value");
        value = value.replace(" ", "");
        if (!value.matches("\\d{9}") || !hasValidCheckDigit(value)) {
            throw new IllegalArgumentException("Organisasjonsnummeret må være ni sifre med gyldig kontrollsiffer.");
        }
    }

    private static boolean hasValidCheckDigit(String digits) {
        var sum = 0;
        for (var i = 0; i < WEIGHTS.length; i++) {
            sum += WEIGHTS[i] * Character.digit(digits.charAt(i), 10);
        }
        var remainder = sum % 11;
        var checkDigit = remainder == 0 ? 0 : 11 - remainder;
        return checkDigit != 10 && checkDigit == Character.digit(digits.charAt(8), 10);
    }
}
