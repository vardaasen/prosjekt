package no.fagskolen.prosjekt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import com.vaadin.flow.shared.ApplicationConstants;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GrantedAuthoritiesMapper keycloakRoleMapper,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                // Vaadin er kartlagt på servlet-roten (vaadin.url-mapping="/*", standard),
                // så rammeverkets interne init/uidl/heartbeat-forespørsler (markert med
                // spørreparameteren "v-r") sendes uten CSRF-token slik Vaadins klient selv
                // håndterer det internt. Uten dette unntaket blokkerer Spring Securitys
                // CSRF-filter disse forespørslene før de når kontrolleren, som viste seg
                // som en evig "Connection lost"-reconnect-løkke etter innlogging på
                // /admin og /app. Se docs/local-development.md.
                .csrf(csrf -> csrf.ignoringRequestMatchers(request ->
                        request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER) != null))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/app", "/app/**").hasRole("SELLER")
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/selgersoknad", "/selgersoknad/**").authenticated()
                        .requestMatchers("/tilgang-nektet", "/innlogging-feilet").permitAll()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect("/innlogging-feilet"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(keycloakRoleMapper)))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedPage("/tilgang-nektet"))
                .logout(logout -> logout
                        // Enkel lenke-basert utlogging i Vaadin og server-renderte sider:
                        // GET er ikke CSRF-beskyttet i utgangspunktet, så dette holder
                        // utloggingen konsistent på tvers av begge UI-lagene i demoen.
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                        // Uten RP-initiated logout mot Keycloak overlever Keycloaks
                        // egen SSO-økt selv om den lokale Spring-økten avsluttes: neste
                        // innlogging (f.eks. som en annen demobruker) hopper stille over
                        // Keycloaks innloggingsskjema og gjenbruker forrige identitet.
                        // oidcLogoutSuccessHandler sender brukeren via Keycloaks
                        // end_session_endpoint slik at også SSO-økten avsluttes.
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)));
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
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
