package no.fagskolen.prosjekt.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import no.fagskolen.prosjekt.marketplace.affiliations.Affiliations;
import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.listings.BusinessListings;
import no.fagskolen.prosjekt.marketplace.people.PersonDirectory;
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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private Affiliations affiliations;

    @Autowired
    private PersonDirectory personDirectory;

    @Autowired
    private BusinessListings businessListings;

    @Test
    void constructsWithoutThrowing() {
        assertThatCode(() -> new AdminWorkspaceView(sellerApplications, affiliations, personDirectory, businessListings))
                .doesNotThrowAnyException();
    }

    @Test
    void keepsAffiliationsAndTheOldSellerApplicationsClearlyApart() {
        var view = new AdminWorkspaceView(sellerApplications, affiliations, personDirectory, businessListings);

        assertThat(headings(view)).containsSubsequence(
                "Annonser til godkjenning",
                "Alle annonser",
                "Tilknytninger som venter",
                "Alle tilknytninger",
                "Selgersøknader (gammel ordning, fjernes)");
    }

    private static List<String> headings(Component root) {
        return Stream.concat(Stream.of(root), root.getChildren().flatMap(child -> Stream.concat(Stream.of(child),
                        child.getChildren())))
                .filter(component -> component instanceof HasText && component.getElement().getTag().equals("h2"))
                .map(component -> component.getElement().getText())
                .toList();
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
