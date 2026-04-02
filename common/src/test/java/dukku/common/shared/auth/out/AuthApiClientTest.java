package dukku.common.shared.auth.out;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiClientTest {

    private static final String PROPERTY_NAME = "INTERNAL_SERVICE_TOKEN";

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(PROPERTY_NAME);
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("세션 폐기 요청은 내부 auth revoke endpoint에 DELETE로 전달된다")
    void revokesSessionsViaDeleteEndpoint() {
        UUID userUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        System.setProperty(PROPERTY_NAME, "internal-token");

        server.createContext("/api/v1/internal/auth/users/" + userUuid + "/sessions", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("DELETE");
            assertThat(exchange.getRequestHeaders().getFirst("X-Internal-Service-Token")).isEqualTo("internal-token");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        AuthApiClient client = new AuthApiClient(baseUrl);

        client.revokeAllSessions(userUuid);
    }
}
