package no.fagskolen.prosjekt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GrantedAuthoritiesMapper keycloakRoleMapper) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/app", "/app/**").hasRole("SELLER")
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(keycloakRoleMapper)))
                .logout(logout -> logout.logoutSuccessUrl("/"));
        return http.build();
    }

    @Bean
    GrantedAuthoritiesMapper keycloakRoleMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new LinkedHashSet<>(authorities);

            authorities.stream()
                    .filter(OidcUserAuthority.class::isInstance)
                    .map(OidcUserAuthority.class::cast)
                    .map(authority -> authority.getIdToken().getClaimAsMap("realm_access"))
                    .filter(realmAccess -> realmAccess != null)
                    .map(realmAccess -> realmAccess.get("roles"))
                    .filter(Collection.class::isInstance)
                    .flatMap(roles -> ((Collection<?>) roles).stream())
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .forEach(mappedAuthorities::add);

            return mappedAuthorities;
        };
    }
}
