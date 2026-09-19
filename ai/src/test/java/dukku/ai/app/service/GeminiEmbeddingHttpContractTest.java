package dukku.ai.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class GeminiEmbeddingHttpContractTest {

    private static final int DIMENSIONS = 1536;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void nativeSdkTransmitsTaskTypeAndOutputDimensionsForQueryAndDocument() throws Exception {
        try (FakeEmbeddingServer server = new FakeEmbeddingServer()) {
            try (Client client = Client.builder()
                    .apiKey("local-embedding-contract-test-key")
                    .vertexAI(false)
                    .httpOptions(HttpOptions.builder()
                            .baseUrl("http://127.0.0.1:" + server.port())
                            .apiVersion("v1beta")
                            .timeout(10_000)
                            .build())
                    .build()) {
                GeminiEmbeddingService service = new GeminiEmbeddingService(
                        client, "gemini-embedding-001", DIMENSIONS);

                float[] query = service.embedQuery("synthetic contract phrase");
                float[] document = service.embedDocument("synthetic contract phrase");

                assertThat(query).hasSize(DIMENSIONS);
                assertThat(document).hasSize(DIMENSIONS);
                assertUnitNorm(query);
                assertUnitNorm(document);
                assertThat(service.profile()).isEqualTo(
                        "gemini-embedding-001:1536:retrieval-document:normalization-v1");
                assertThat(server.requests).hasSize(2);

                JsonNode queryRequest = server.requests.getFirst();
                JsonNode documentRequest = server.requests.getLast();
                assertThat(queryRequest.toString()).contains("\"taskType\":\"RETRIEVAL_QUERY\"");
                assertThat(documentRequest.toString()).contains("\"taskType\":\"RETRIEVAL_DOCUMENT\"");
                assertThat(queryRequest.toString()).contains("\"outputDimensionality\":1536");
                assertThat(documentRequest.toString()).contains("\"outputDimensionality\":1536");
                assertThat(queryRequest.toString()).contains("synthetic contract phrase");
                assertThat(documentRequest.toString()).contains("synthetic contract phrase");
                assertThat(server.paths).allSatisfy(path -> assertThat(path).isEqualTo(
                        "/v1beta/models/gemini-embedding-001:batchEmbedContents"));
            }
        }
    }

    private static void assertUnitNorm(float[] values) {
        double squaredNorm = 0.0;
        for (float value : values) {
            assertThat(Float.isFinite(value)).isTrue();
            squaredNorm += (double) value * value;
        }
        assertThat(Math.sqrt(squaredNorm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1.0e-6));
    }

    private static final class FakeEmbeddingServer implements AutoCloseable {
        private static final String API_KEY = "local-embedding-contract-test-key";
        private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
        private final List<String> paths = new CopyOnWriteArrayList<>();
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        private FakeEmbeddingServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.setExecutor(executor);
            server.start();
        }

        private int port() {
            return server.getAddress().getPort();
        }

        private void handle(HttpExchange exchange) throws IOException {
            paths.add(exchange.getRequestURI().getPath());
            requests.add(OBJECT_MAPPER.readTree(exchange.getRequestBody()));
            byte[] response = embeddingResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }

        private static String embeddingResponse() throws IOException {
            List<Float> values = new ArrayList<>(DIMENSIONS);
            for (int i = 0; i < DIMENSIONS; i++) {
                values.add(i == 0 ? 2.0f : 1.0f);
            }
            return OBJECT_MAPPER.writeValueAsString(java.util.Map.of(
                    "embeddings", List.of(java.util.Map.of("values", values))));
        }
    }
}
