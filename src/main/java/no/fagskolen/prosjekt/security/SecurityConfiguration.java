package no.fagskolen.prosjekt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

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
                        // Ekstra vern ved direkte sidelasting av administrasjonen. Selve
                        // tilgangskontrollen i Vaadin er @RolesAllowed på visningene,
                        // håndhevet av VaadinSecurityConfigurer nedenfor.
                        .requestMatchers("/app/admin", "/app/admin/**").hasRole("ADMIN")
                        .requestMatchers("/selgersoknad", "/selgersoknad/**").authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect("/innlogging-feilet"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(keycloakRoleMapper)))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedPage("/tilgang-nektet"))
                // Vaadin er kartlagt på /app/* og sikres med Vaadins egen integrasjon:
                // - @RolesAllowed håndheves ved hver navigasjon (NavigationAccessControl).
                //   URL-regler ser bare første sidelasting; navigasjon inne i Vaadin går
                //   som interne forespørsler, så uten dette kunne hvem som helst navigere
                //   klientside til administrasjonen.
                // - Vaadins interne forespørsler slippes gjennom og unntas fra Spring
                //   CSRF; de beskyttes av Vaadins egen sikkerhetsnøkkel.
                // - Utlogging er standard Spring Security: bare POST /logout med
                //   CSRF-token (LogoutForm i Vaadin, skjema i Thymeleaf). GET-utlogging
                //   ville latt et annet nettsted logge brukeren ut.
                // - Utlogging går via Keycloaks end_session_endpoint (RP-initiated
                //   logout), slik at også Keycloaks SSO-økt avsluttes. Ellers hopper
                //   neste innlogging stille over Keycloak-skjemaet og gjenbruker
                //   forrige identitet.
                // Offentlige Spring MVC-sider utenfor /app er åpne (anyRequest).
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin
                        .oauth2LoginPage("/oauth2/authorization/keycloak", "{baseUrl}/")
                        .anyRequest(AuthorizedUrl::permitAll));
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
