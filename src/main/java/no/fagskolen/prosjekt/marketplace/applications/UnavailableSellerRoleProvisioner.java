package no.fagskolen.prosjekt.marketplace.applications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UnavailableSellerRoleProvisioner {

    @Bean
    @ConditionalOnMissingBean(SellerRoleProvisioner.class)
    SellerRoleProvisioner unavailableSellerRoleProvisionerBean() {
        return oidcSubject -> {
            throw new IllegalStateException(
                    "Seller approval requires a configured Keycloak administrator service account.");
        };
    }
}
