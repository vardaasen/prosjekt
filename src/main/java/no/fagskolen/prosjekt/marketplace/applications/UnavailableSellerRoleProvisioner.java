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
            throw new SellerRoleProvisioningException(
                    "Rolletildeling er ikke konfigurert. Kontakt driftsansvarlig før søknaden godkjennes.");
        };
    }
}
