package no.fagskolen.prosjekt.admin;

import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.applications.SellerRoleProvisioner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Diagnostic smoke test: constructs AdminWorkspaceView directly against the
 * real Spring context, outside of a browser, to catch any server-side
 * exception thrown during view construction (which would otherwise only
 * surface as Vaadin's dev-mode error overlay in a real browser).
 */
@SpringBootTest
@Testcontainers
@Import(AdminWorkspaceViewSmokeTest.RoleProvisionerConfiguration.class)
class AdminWorkspaceViewSmokeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private SellerApplications sellerApplications;

    @Test
    void constructsWithoutThrowing() {
        assertThatCode(() -> new AdminWorkspaceView(sellerApplications)).doesNotThrowAnyException();
    }

    @TestConfiguration
    static class RoleProvisionerConfiguration {
        @Bean
        @Primary
        SellerRoleProvisioner sellerRoleProvisioner() {
            return oidcSubject -> {
            };
        }
    }
}
