package no.fagskolen.prosjekt.seo;

import no.fagskolen.prosjekt.ProsjektApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void redirectsAnonymousSellerWorkspaceRequestsToKeycloak() throws Exception {
        mockMvc.perform(get("/app"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void redirectsAnonymousAdministrationRequestsToKeycloak() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void preventsSellersFromOpeningTheAdministrationWorkspace() throws Exception {
        mockMvc.perform(get("/admin").with(user("seller").roles("SELLER")))
                .andExpect(status().isForbidden());
    }
}
