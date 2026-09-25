package no.fagskolen.prosjekt.marketplace.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feilmeldingene følger docs/design/feilmeldinger.md: beskriv hva som er galt
 * og hvordan det rettes, med ulike meldinger for ulike feil.
 */
class OrganisationNumberMessagesTest {

    @Test
    void acceptsAValidNumberWithSpaces() {
        assertThat(OrganisationNumberMessages.errorFor("974 760 673")).isEmpty();
    }

    @Test
    void explainsTheFormatWhenTheNumberIsNotNineDigits() {
        assertThat(OrganisationNumberMessages.errorFor("97476067"))
                .contains("Organisasjonsnummeret må være ni sifre.");
        assertThat(OrganisationNumberMessages.errorFor("97476067A"))
                .contains("Organisasjonsnummeret må være ni sifre.");
    }

    @Test
    void asksThePersonToCheckTheDigitsWhenTheCheckDigitDoesNotMatch() {
        assertThat(OrganisationNumberMessages.errorFor("974760674"))
                .contains("Sjekk at organisasjonsnummeret er skrevet riktig.");
    }
}
