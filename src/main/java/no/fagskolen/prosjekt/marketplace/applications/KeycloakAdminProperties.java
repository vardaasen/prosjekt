package no.fagskolen.prosjekt.marketplace.applications;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak-admin")
public record KeycloakAdminProperties(
        boolean enabled,
        @NotBlank String baseUrl,
        @NotBlank String realm,
        @NotBlank String clientId,
        @NotBlank String clientSecret) {
}
