package no.fagskolen.prosjekt.marketplace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerAccountTest {

    @Test
    void requiresAStableOidcSubject() {
        assertThatThrownBy(() -> new SellerAccount("  ", new Seller("Fjord Drift AS", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("oidcSubject must not be blank");
    }

    @Test
    void normalizesTheOidcSubject() {
        var account = new SellerAccount("  keycloak-subject  ", new Seller("Fjord Drift AS", false));

        assertThat(account.oidcSubject()).isEqualTo("keycloak-subject");
    }
}
