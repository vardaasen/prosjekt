package no.fagskolen.prosjekt.marketplace.applications;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakSellerRoleProvisionerTest {

    private HttpServer server;
    private List<String> requests;

    @BeforeEach
    void startServer() throws IOException {
        requests = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void grantsTheSellerRealmRoleToTheOidcSubject() {
        var properties = new KeycloakAdminProperties(
                true,
                "http://localhost:" + server.getAddress().getPort(),
                "havbruksbrukt",
                "marketplace-provisioner",
                "not-a-real-secret");
        var provisioner = new KeycloakSellerRoleProvisioner(properties);

        provisioner.grantSellerRole("subject-123");

        assertThat(requests).containsExactly(
                "POST /realms/havbruksbrukt/protocol/openid-connect/token",
                "GET /admin/realms/havbruksbrukt/users/subject-123 Bearer token",
                "GET /admin/realms/havbruksbrukt/roles/SELLER Bearer token",
                "POST /admin/realms/havbruksbrukt/users/user-123/role-mappings/realm Bearer token");
    }

    private void respond(HttpExchange exchange) throws IOException {
        var request = exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath();
        var authorization = exchange.getRequestHeaders().getFirst("Authorization");
        requests.add(authorization == null ? request : request + " " + authorization);

        var path = exchange.getRequestURI().getPath();
        if (path.endsWith("/token")) {
            respondJson(exchange, 200, "{\"access_token\":\"token\"}");
        } else if (path.endsWith("/users/subject-123")) {
            respondJson(exchange, 200, "{\"id\":\"user-123\"}");
        } else if (path.endsWith("/roles/SELLER")) {
            respondJson(exchange, 200, "{\"id\":\"seller-role-id\",\"name\":\"SELLER\"}");
        } else if (path.endsWith("/role-mappings/realm")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
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
