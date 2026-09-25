package no.fagskolen.prosjekt.marketplace.people;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import no.fagskolen.prosjekt.marketplace.applications.KeycloakAdminProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Administratoren ser navn og e-post ved oppslag i Keycloak når saken vurderes;
 * ingenting kopieres til markedsplassens database (0009, #18).
 */
class KeycloakPersonDirectoryTest {

    private HttpServer server;
    private int userStatus;
    private String userBody;

    @BeforeEach
    void startServer() throws IOException {
        userStatus = 200;
        userBody = "{\"id\":\"subject-123\",\"username\":\"kari@fjord.no\",\"firstName\":\"Kari\","
                + "\"lastName\":\"Nordmann\",\"email\":\"kari@fjord.no\"}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void looksUpNameAndEmailForAnOidcSubject() {
        assertThat(directory().lookup("subject-123"))
                .contains(new PersonContact("Kari Nordmann", "kari@fjord.no"));
    }

    @Test
    void fallsBackToTheUsernameWhenNoNameIsGiven() {
        userBody = "{\"id\":\"subject-123\",\"username\":\"kari@fjord.no\",\"email\":\"kari@fjord.no\"}";

        assertThat(directory().lookup("subject-123"))
                .contains(new PersonContact("kari@fjord.no", "kari@fjord.no"));
    }

    @Test
    void returnsNothingWhenThePersonIsUnknownOrKeycloakFails() {
        userStatus = 404;
        assertThat(directory().lookup("subject-123")).isEmpty();

        userStatus = 500;
        assertThat(directory().lookup("subject-123")).isEmpty();
    }

    private KeycloakPersonDirectory directory() {
        return new KeycloakPersonDirectory(new KeycloakAdminProperties(
                true,
                "http://localhost:" + server.getAddress().getPort(),
                "havbruksbrukt",
                "marketplace-provisioner",
                "not-a-real-secret"));
    }

    private void respond(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (path.endsWith("/token")) {
            respondJson(exchange, 200, "{\"access_token\":\"token\"}");
        } else if (path.equals("/admin/realms/havbruksbrukt/users/subject-123")) {
            respondJson(exchange, userStatus, userStatus == 200 ? userBody : "{}");
        } else {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        }
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
