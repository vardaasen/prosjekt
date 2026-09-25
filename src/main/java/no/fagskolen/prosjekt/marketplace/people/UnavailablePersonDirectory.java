package no.fagskolen.prosjekt.marketplace.people;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
class UnavailablePersonDirectory {

    @Bean
    @ConditionalOnMissingBean(PersonDirectory.class)
    PersonDirectory unavailablePersonDirectoryBean() {
        return oidcSubject -> Optional.empty();
    }
}
