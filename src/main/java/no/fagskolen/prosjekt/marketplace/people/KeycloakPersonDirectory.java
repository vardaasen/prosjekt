package no.fagskolen.prosjekt.marketplace.people;

import no.fagskolen.prosjekt.marketplace.applications.KeycloakAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Slår opp navn og e-post i Keycloak med den minst-privilegerte service-kontoen
 * (view-users). Resultatet vises bare og lagres ikke.
 */
@Component
@ConditionalOnProperty(prefix = "app.keycloak-admin", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KeycloakAdminProperties.class)
class KeycloakPersonDirectory implements PersonDirectory {

    private static final Logger log = LoggerFactory.getLogger(KeycloakPersonDirectory.class);

    private final RestClient client;
    private final KeycloakAdminProperties properties;

    KeycloakPersonDirectory(KeycloakAdminProperties properties) {
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<PersonContact> lookup(String oidcSubject) {
        try {
            var user = (Map<String, Object>) client.get()
                    .uri("/admin/realms/{realm}/users/{oidcSubject}", properties.realm(), oidcSubject)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(Map.class);
            if (user == null) {
                return Optional.empty();
            }
            var name = (text(user, "firstName") + " " + text(user, "lastName")).trim();
            var displayName = name.isEmpty() ? text(user, "username") : name;
            return Optional.of(new PersonContact(displayName, text(user, "email")));
        } catch (RestClientException exception) {
            log.warn("Kunne ikke slå opp person {} i Keycloak: {}", oidcSubject, exception.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private String accessToken() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        var response = client.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        return text((Map<String, Object>) response, "access_token");
    }

    private static String text(Map<String, Object> values, String key) {
        return values != null && values.get(key) instanceof String value ? value : "";
    }
}
