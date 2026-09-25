package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrganisationNumberTest {

    @Test
    void acceptsANineDigitNumberWithAValidCheckDigit() {
        // Brønnøysundregistrene, kontrollsiffer 3 etter modulus 11.
        assertThat(new OrganisationNumber("974760673").value()).isEqualTo("974760673");
    }

    @Test
    void ignoresSpacesUsedForReadability() {
        assertThat(new OrganisationNumber(" 974 760 673 ").value()).isEqualTo("974760673");
    }

    @ParameterizedTest
    @ValueSource(strings = {"974760674", "97476067", "9747606731", "97476067A", ""})
    void rejectsNumbersThatAreNotValidNorwegianOrganisationNumbers(String candidate) {
        assertThatIllegalArgumentException().isThrownBy(() -> new OrganisationNumber(candidate));
    }
}
