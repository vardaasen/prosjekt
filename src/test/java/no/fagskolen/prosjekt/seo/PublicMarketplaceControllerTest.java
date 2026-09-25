package no.fagskolen.prosjekt.seo;

import no.fagskolen.prosjekt.ProsjektApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = ProsjektApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.public-base-url=http://localhost")
@Testcontainers
class PublicMarketplaceControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rendersCanonicalAndIndexableCatalogueForDefaultRoute() throws Exception {
        mockMvc.perform(get("/utstyr"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"http://localhost/utstyr\">")))
                .andExpect(content().string(containsString(
                        "<meta name=\"robots\" content=\"index,follow\">")));
    }

    @Test
    void rendersIndexableCanonicalHomepage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"http://localhost\">")))
                .andExpect(content().string(containsString(
                        "<meta name=\"robots\" content=\"index,follow\">")));
    }

    @Test
    void rendersSellerEntryPageWithClearAccountAndLoginGuidance() throws Exception {
        mockMvc.perform(get("/selg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Har du allerede en konto?")))
                .andExpect(content().string(containsString("href=\"/selgersoknad\"")))
                .andExpect(content().string(containsString("Selvregistrering er ikke åpnet ennå.")))
                .andExpect(content().string(containsString(
                        "<meta name=\"robots\" content=\"noindex,follow\">")));
    }

    @Test
    void doesNotUseUntrustedHostHeaderForCanonicalUrl() throws Exception {
        mockMvc.perform(get("/utstyr").header("Host", "evil.example"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"http://localhost/utstyr\">")))
                .andExpect(content().string(not(containsString("evil.example"))));
    }

    @Test
    void marksFilteredNullResultAsNoindexAndKeepsCanonicalStable() throws Exception {
        mockMvc.perform(get("/utstyr").param("q", "ukjent"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"http://localhost/utstyr\">")))
                .andExpect(content().string(containsString(
                        "<meta name=\"robots\" content=\"noindex,follow\">")))
                .andExpect(content().string(containsString("Ingen annonser matcher søket.")));
    }

    @Test
    void exposesCrawlerEntryPoints() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /logg-inn")))
                .andExpect(content().string(containsString("Sitemap: http://localhost/sitemap.xml")));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/xml")))
                .andExpect(content().string(containsString("/utstyr/sentrifugalpumpe-450")))
                .andExpect(content().string(not(containsString("/app"))));
    }

    @Test
    void rendersUsefulNotFoundPage() throws Exception {
        mockMvc.perform(get("/utstyr/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Fant ikke annonsen")))
                .andExpect(content().string(containsString("href=\"/utstyr\"")));
    }

    @Test
    void redirectsAnonymousAdministrationRequestsToKeycloak() throws Exception {
        mockMvc.perform(get("/app/admin").servletPath("/app"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void redirectsAnonymousSellerApplicationRequestsToKeycloak() throws Exception {
        mockMvc.perform(get("/selgersoknad"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void rendersSellerApplicationAsServerRenderedPageForAuthenticatedUsers() throws Exception {
        mockMvc.perform(get("/selgersoknad").with(oidcLogin().idToken(token -> token.subject("buyer-subject"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Søk som selger</h1>")))
                .andExpect(content().string(containsString("name=\"sellerName\"")))
                .andExpect(content().string(containsString("<form method=\"post\" action=\"/logout\">")))
                .andExpect(content().string(not(containsString("href=\"/logout\""))))
                .andExpect(content().string(not(containsString("vaadin-"))));
    }

    @Test
    void redirectsOidcLogoutThroughKeycloakEndSessionEndpoint() throws Exception {
        mockMvc.perform(post("/logout").with(keycloakOidcLogin()).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString(
                        "http://localhost:8180/realms/havbruksbrukt/protocol/openid-connect/logout")))
                .andExpect(header().string("Location", containsString("post_logout_redirect_uri=http://localhost/")))
                .andExpect(header().string("Location", containsString("id_token_hint=")));
    }

    @Test
    void rejectsLogoutWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/logout").with(keycloakOidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsLogoutWithVaadinRequestTypeParameterButNoCsrfToken() throws Exception {
        mockMvc.perform(post("/logout?v-r=uidl").with(keycloakOidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsSellerApplicationWithVaadinRequestTypeParameterButNoCsrfToken() throws Exception {
        mockMvc.perform(post("/selgersoknad?v-r=uidl")
                        .with(oidcLogin().idToken(token -> token.subject("forged-subject")))
                        .param("sellerName", "Forfalsket AS")
                        .param("sellerLocation", "Narvik"))
                .andExpect(status().isForbidden());
    }

    @Test
    void keepsVaadinInternalRequestsExemptFromSpringCsrf() throws Exception {
        // Vaadin beskytter sine egne UIDL-forespørsler med egen sikkerhetsnøkkel.
        // Blokkerer Spring CSRF dem, oppstår "Connection lost"-løkken fra PR #1.
        // MockMvc har ingen Vaadin-servlet, så svaret kommer fra DispatcherServlet;
        // poenget er at Spring Security slapp forespørselen gjennom uten CSRF-token.
        mockMvc.perform(post("/app/?v-r=uidl").servletPath("/app").with(keycloakOidcLogin()))
                .andExpect(status().is(not(403)));
    }

    @Test
    void returnsNotFoundForUnknownPublicPathsInsteadOfTheVaadinApp() throws Exception {
        mockMvc.perform(get("/finnes-ikke"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("vaadin"))));
    }

    @Test
    void doesNotLogOutOnCrossSiteGetNavigation() throws Exception {
        mockMvc.perform(get("/logout").with(keycloakOidcLogin()))
                .andExpect(header().doesNotExist("Location"));
    }

    private static OidcLoginRequestPostProcessor keycloakOidcLogin() {
        var loginRegistration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("marketplace")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://example.invalid/auth")
                .tokenUri("https://example.invalid/token")
                .userInfoUri("https://example.invalid/userinfo")
                .jwkSetUri("https://example.invalid/jwks")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build();

        return oidcLogin()
                .clientRegistration(loginRegistration)
                .idToken(token -> token.subject("buyer-subject"));
    }

    @Test
    void submitsSellerApplicationWithCsrfProtection() throws Exception {
        mockMvc.perform(post("/selgersoknad")
                        .with(oidcLogin().idToken(token -> token.subject("new-buyer-subject")))
                        .with(csrf())
                        .param("sellerName", "Ny Drift AS")
                        .param("sellerLocation", "Narvik"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/selgersoknad"));
    }

    @Test
    void explainsWhySellersCannotOpenTheAdministrationWorkspace() throws Exception {
        mockMvc.perform(get("/app/admin").servletPath("/app").with(user("seller").roles("SELLER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void offersLoginThatLandsOnMinSideForAnonymousVisitors() throws Exception {
        mockMvc.perform(get("/utstyr"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/logg-inn\" rel=\"nofollow\">Logg inn</a>")))
                .andExpect(content().string(not(containsString("Logg ut"))));
    }

    @Test
    void showsLogoutInsteadOfLoginToLoggedInPersons() throws Exception {
        mockMvc.perform(get("/utstyr").with(oidcLogin().idToken(token -> token.subject("buyer-subject"))))
                .andExpect(content().string(containsString("href=\"/selg\">Selg utstyr</a>")))
                .andExpect(content().string(containsString("href=\"/app\">Min side</a>")))
                .andExpect(content().string(containsString("<form method=\"post\" action=\"/logout\">")))
                .andExpect(content().string(not(containsString("Logg inn</a>"))))
                .andExpect(content().string(not(containsString("Min søknad"))));
    }

    @Test
    void sendsMarketplaceAdministratorsFromPublicPagesToAdministration() throws Exception {
        // Administratoren er en egen innlogging som ikke surfer (0010).
        for (var page : new String[] {"/", "/utstyr", "/utstyr/sentrifugalpumpe-450", "/selg", "/selgersoknad"}) {
            mockMvc.perform(get(page).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/app/admin"));
        }
    }

    @Test
    void doesNotRedirectMarketplaceAdministratorsFromCrawlerAndErrorEndpoints() throws Exception {
        for (var endpoint : new String[] {"/robots.txt", "/sitemap.xml", "/tilgang-nektet", "/innlogging-feilet"}) {
            mockMvc.perform(get(endpoint).with(user("admin").roles("ADMIN")))
                    .andExpect(redirectedUrl(null));
        }
    }

    @Test
    void asksAnonymousVisitorsToLogInBeforeReturning() throws Exception {
        mockMvc.perform(get("/logg-inn").param("returnTo", "/utstyr"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void landsOnMinSideAfterAPlainLogin() throws Exception {
        mockMvc.perform(get("/logg-inn").with(oidcLogin().idToken(token -> token.subject("buyer-subject"))))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/app"));
    }

    @Test
    void returnsLoggedInUsersToTheLocalPageTheyCameFrom() throws Exception {
        mockMvc.perform(get("/logg-inn").param("returnTo", "/utstyr/sentrifugalpumpe-450")
                        .with(oidcLogin().idToken(token -> token.subject("buyer-subject"))))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/utstyr/sentrifugalpumpe-450"));
    }

    @Test
    void sendsMarketplaceAdministratorsStraightToAdministrationAfterLogin() throws Exception {
        mockMvc.perform(get("/logg-inn").param("returnTo", "/utstyr").with(user("admin").roles("ADMIN")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/app/admin"));
    }

    @Test
    void refusesToReturnToOtherSites() throws Exception {
        for (var returnTo : new String[] {"//evil.example/x", "https://evil.example", "/\\evil.example", "utstyr"}) {
            mockMvc.perform(get("/logg-inn").param("returnTo", returnTo)
                            .with(oidcLogin().idToken(token -> token.subject("buyer-subject"))))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/"));
        }
    }

    @Test
    void rendersSafeLoginFailureGuidance() throws Exception {
        mockMvc.perform(get("/innlogging-feilet"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Innloggingen kunne ikke fullføres")))
                .andExpect(content().string(containsString("Prøv å logge inn igjen")))
                .andExpect(content().string(not(containsString("exception"))));
    }

    @Test
    void rendersSafeAccessDeniedGuidance() throws Exception {
        mockMvc.perform(get("/tilgang-nektet"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Du har ikke tilgang til denne siden")))
                .andExpect(content().string(containsString("Til forsiden")));
    }
}
