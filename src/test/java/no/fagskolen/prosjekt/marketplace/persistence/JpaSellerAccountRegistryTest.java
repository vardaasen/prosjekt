package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.accounts.SellerAccountRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JpaSellerAccountRegistryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private SellerAccountRegistry sellerAccountRegistry;

    @Autowired
    private SellerAccountJpaRepository sellerAccounts;

    @Test
    void createsAnAccountOnceForAnOidcSubject() {
        var first = sellerAccountRegistry.registerSellerAccount("subject-123", "Fjord Drift AS");
        var second = sellerAccountRegistry.registerSellerAccount("subject-123", "Changed display name");

        assertThat(first).isEqualTo(second);
        assertThat(sellerAccounts.count()).isEqualTo(1);
        assertThat(first.seller().name()).isEqualTo("Fjord Drift AS");
        assertThat(first.seller().verified()).isFalse();
    }
}
