package no.fagskolen.prosjekt.security;

import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import no.fagskolen.prosjekt.admin.AdminWorkspaceView;
import no.fagskolen.prosjekt.marketplace.ui.SellerWorkspaceView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * URL-regler ser bare første sidelasting. Navigasjon inne i Vaadin går som
 * interne forespørsler, så {@code @RolesAllowed} på visningene må håndheves av
 * Vaadins NavigationAccessControl. Den er av som standard og slås bare på av
 * VaadinSecurityConfigurer. Uten den kunne en anonym bruker navigere klientside
 * til administrasjonsvisningen.
 */
@SpringBootTest
@Testcontainers
class VaadinNavigationAccessControlTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private NavigationAccessControl navigationAccessControl;

    @Autowired
    private AccessAnnotationChecker accessAnnotationChecker;

    @Test
    void enforcesRolesAllowedOnEveryVaadinNavigation() {
        assertThat(navigationAccessControl.isEnabled()).isTrue();
    }

    @Test
    void onlyAdministratorsMayOpenTheAdministrationWorkspace() {
        assertThat(hasAccess(AdminWorkspaceView.class, null)).isFalse();
        assertThat(hasAccess(AdminWorkspaceView.class, "SELLER")).isFalse();
        assertThat(hasAccess(AdminWorkspaceView.class, "BUYER")).isFalse();
        assertThat(hasAccess(AdminWorkspaceView.class, "ADMIN")).isTrue();
    }

    @Test
    void onlySellersMayOpenTheSellerWorkspace() {
        assertThat(hasAccess(SellerWorkspaceView.class, null)).isFalse();
        assertThat(hasAccess(SellerWorkspaceView.class, "BUYER")).isFalse();
        assertThat(hasAccess(SellerWorkspaceView.class, "SELLER")).isTrue();
    }

    private boolean hasAccess(Class<?> view, String role) {
        Principal principal = role == null ? null : () -> "demo-" + role.toLowerCase();
        return accessAnnotationChecker.hasAccess(view, principal, checkedRole -> checkedRole.equals(role));
    }
}
