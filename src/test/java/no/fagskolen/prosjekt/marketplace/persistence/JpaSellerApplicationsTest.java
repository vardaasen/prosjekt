package no.fagskolen.prosjekt.marketplace.persistence;

import no.fagskolen.prosjekt.marketplace.applications.SellerApplications;
import no.fagskolen.prosjekt.marketplace.applications.SellerRoleProvisioner;
import no.fagskolen.prosjekt.marketplace.domain.Seller;
import no.fagskolen.prosjekt.marketplace.domain.SellerApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SpringBootTest
@Testcontainers
@Import(JpaSellerApplicationsTest.RoleProvisionerConfiguration.class)
class JpaSellerApplicationsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private SellerApplications sellerApplications;

    @Autowired
    private SellerAccountJpaRepository sellerAccounts;

    @Autowired
    private RecordingSellerRoleProvisioner sellerRoleProvisioner;

    @BeforeEach
    void resetRoleProvisioner() {
        sellerRoleProvisioner.reset();
    }

    @Test
    void approvesAnApplicationOnlyAfterGrantingTheSellerRoleAndActivatesTheSellerAccount() {
        var submitted = sellerApplications.submit(
                "seller-subject",
                new Seller("Fjord Drift AS", false, "Frøya"));

        var approved = sellerApplications.approve(submitted.id(), "administrator-subject");

        assertThat(approved.status()).isEqualTo(SellerApplicationStatus.APPROVED);
        assertThat(approved.decidedBy()).isEqualTo("administrator-subject");
        assertThat(sellerRoleProvisioner.grantedSubjects()).contains("seller-subject");
        var account = sellerAccounts.findById("seller-subject").orElseThrow();
        assertThat(account.sellerName()).isEqualTo("Fjord Drift AS");
        assertThat(account.sellerLocation()).isEqualTo("Frøya");
        assertThat(account.verifiedSeller()).isTrue();
        assertThat(sellerApplications.findAuditTrail(submitted.id()))
                .extracting(entry -> entry.action())
                .containsExactly("SUBMITTED", "APPROVED");
    }

    @Test
    void rejectsAnApplicationWithAnAuditableReason() {
        var submitted = sellerApplications.submit(
                "rejected-subject",
                new Seller("Avslag AS", false, "Bergen"));

        var rejected = sellerApplications.reject(
                submitted.id(),
                "administrator-subject",
                "Virksomhetsopplysningene må kompletteres.");

        assertThat(rejected.status()).isEqualTo(SellerApplicationStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Virksomhetsopplysningene må kompletteres.");
        assertThat(sellerRoleProvisioner.grantedSubjects()).doesNotContain("rejected-subject");
        assertThat(sellerApplications.findAuditTrail(submitted.id()))
                .extracting(entry -> entry.action())
                .containsExactly("SUBMITTED", "REJECTED");
    }

    @Test
    void rejectsDuplicatePendingApplicationsAndBlankRejectionReasons() {
        sellerApplications.submit(
                "pending-subject",
                new Seller("Ventende AS", false, "Alesund"));
        var application = sellerApplications.findLatestFor("pending-subject").orElseThrow();

        assertThatIllegalStateException().isThrownBy(() -> sellerApplications.submit(
                "pending-subject",
                new Seller("Ventende AS", false, "Alesund")));
        assertThatIllegalArgumentException().isThrownBy(() -> sellerApplications.reject(
                application.id(),
                "administrator-subject",
                " "));
    }

    @Test
    void keepsTheApplicationPendingWhenRoleProvisioningFails() {
        var submitted = sellerApplications.submit(
                "unavailable-provisioner-subject",
                new Seller("Ufullstendig AS", false, "Tromso"));
        sellerRoleProvisioner.failWhenGranting();

        assertThatIllegalStateException().isThrownBy(() -> sellerApplications.approve(
                submitted.id(),
                "administrator-subject"));

        assertThat(sellerApplications.findLatestFor("unavailable-provisioner-subject").orElseThrow().status())
                .isEqualTo(SellerApplicationStatus.PENDING);
        assertThat(sellerApplications.findAuditTrail(submitted.id()))
                .extracting(entry -> entry.action())
                .containsExactly("SUBMITTED");
        assertThat(sellerAccounts.findById("unavailable-provisioner-subject")).isEmpty();
    }

    @TestConfiguration
    static class RoleProvisionerConfiguration {

        @Bean
        @Primary
        RecordingSellerRoleProvisioner sellerRoleProvisioner() {
            return new RecordingSellerRoleProvisioner();
        }
    }

    static class RecordingSellerRoleProvisioner implements SellerRoleProvisioner {

        private final List<String> grantedSubjects = new ArrayList<>();
        private boolean failWhenGranting;

        @Override
        public void grantSellerRole(String oidcSubject) {
            if (failWhenGranting) {
                throw new IllegalStateException("Keycloak role provisioning is unavailable.");
            }
            grantedSubjects.add(oidcSubject);
        }

        List<String> grantedSubjects() {
            return grantedSubjects;
        }

        void failWhenGranting() {
            failWhenGranting = true;
        }

        void reset() {
            grantedSubjects.clear();
            failWhenGranting = false;
        }
    }
}
