package no.fagskolen.prosjekt.marketplace.applications;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.keycloak-admin", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KeycloakAdminProperties.class)
class KeycloakSellerRoleProvisioner implements SellerRoleProvisioner {

    private final RestClient client;
    private final KeycloakAdminProperties properties;

    KeycloakSellerRoleProvisioner(KeycloakAdminProperties properties) {
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
    }

    @Override
    public void grantSellerRole(String oidcSubject) {
        try {
            var accessToken = accessToken();
            var userId = userId(oidcSubject, accessToken);
            var sellerRole = sellerRole(accessToken);
            client.post()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm",
                            properties.realm(), userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(List.of(sellerRole))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new SellerRoleProvisioningException(
                    "Kunne ikke tildele selgerrollen akkurat nå. Prøv igjen senere.", exception);
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
        return requiredString(response, "access_token", "Keycloak token response");
    }

    @SuppressWarnings("unchecked")
    private String userId(String oidcSubject, String accessToken) {
        var user = client.get()
                .uri("/admin/realms/{realm}/users/{oidcSubject}", properties.realm(), oidcSubject)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
        return requiredString((Map<String, Object>) user, "id", "Keycloak user response");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sellerRole(String accessToken) {
        var role = client.get()
                .uri("/admin/realms/{realm}/roles/SELLER", properties.realm())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
        requiredString(role, "id", "Keycloak role response");
        requiredString(role, "name", "Keycloak role response");
        return (Map<String, Object>) role;
    }

    private String requiredString(Map<String, Object> response, String field, String responseName) {
        if (response == null || !(response.get(field) instanceof String value) || value.isBlank()) {
            throw new SellerRoleProvisioningException(
                    "Kunne ikke bekrefte selgerrollen hos identitetsleverandøren.");
        }
        return value;
    }
}
