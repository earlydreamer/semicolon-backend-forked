package dukku.ai.global.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class GeminiNativeHttpContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nativeSyncAndStreamFunctionCallRoundTripSignatureAndSaveOnlyFinalAnswer(boolean streaming)
            throws Exception {
        try (FakeGeminiServer server = new FakeGeminiServer()) {
            SingleConnectionDataSource database = new SingleConnectionDataSource(
                    "jdbc:h2:mem:native-gemini-" + streaming, "sa", "", true);
            new ResourceDatabasePopulator(new ClassPathResource(
                    "org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql")).execute(database);
            try {
                JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                        .jdbcTemplate(new JdbcTemplate(database))
                        .build();
                ChatMemory memory = MessageWindowChatMemory.builder()
                        .chatMemoryRepository(repository)
                        .maxMessages(20)
                        .build();
                LocalEchoTool tool = new LocalEchoTool();
                try (Client sdkClient = server.client()) {
                    ChatClient chatClient = chatClient(sdkClient, memory, tool);
                    String conversationId = "native-contract-" + streaming;

                    var request = chatClient.prompt()
                            .user("echo this")
                            .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId));
                    String answer = streaming
                            ? String.join("", request.stream().content().collectList().block(Duration.ofSeconds(20)))
                            : request.call().content();

                    assertThat(answer).isEqualTo("Final answer");
                    assertThat(tool.calls).hasValue(3);
                    assertThat(server.requests).hasSize(3);
                    assertThat(server.paths).hasSize(3).allSatisfy(path -> assertThat(path)
                            .contains(streaming ? ":streamGenerateContent" : ":generateContent")
                            .contains("gemini-3.5-flash-lite"));

                    JsonNode parallelFollowUp = server.requests.get(1);
                    assertThat(parallelFollowUp.findValuesAsText("thoughtSignature"))
                            .containsExactly("AQID", "BwgJ");
                    assertThat(parallelFollowUp.findValues("functionResponse")).hasSize(2);

                    JsonNode sequentialFollowUp = server.requests.getLast();
                    assertThat(sequentialFollowUp.findValuesAsText("thoughtSignature"))
                            .containsExactly("AQID", "BwgJ", "CAkK");
                    assertThat(sequentialFollowUp.findValues("functionResponse")).hasSize(3);

                    List<Message> savedMessages = memory.get(conversationId);
                    assertThat(savedMessages).extracting(Message::getMessageType)
                            .containsExactly(org.springframework.ai.chat.messages.MessageType.USER,
                                    org.springframework.ai.chat.messages.MessageType.ASSISTANT);
                    assertThat(savedMessages).extracting(Message::getText)
                            .containsExactly("echo this", "Final answer");
                    assertThat(savedMessages).noneMatch(message -> message.getText().contains("private reasoning"));
                }
            } finally {
                database.destroy();
            }
        }
    }

    private static ChatClient chatClient(Client client, ChatMemory memory, LocalEchoTool tool) {
        GoogleGenAiChatModel nativeModel = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .options(GoogleGenAiChatOptions.builder()
                        .model(GoogleGenAiChatModel.ChatModel.GEMINI_3_5_FLASH_LITE)
                        .includeThoughts(true)
                        .build())
                .build();
        ThoughtFilteringChatModel filteredModel = new ThoughtFilteringChatModel(nativeModel);
        return ChatClient.builder(filteredModel)
                .defaultTools(tool)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memory).order(10).build(),
                        ToolCallingAdvisor.builder().advisorOrder(300).build())
                .build();
    }

    static final class LocalEchoTool {
        private final AtomicInteger calls = new AtomicInteger();

        @Tool(description = "Echoes one short input")
        public String localEcho(@ToolParam(description = "The text to echo") String text) {
            calls.incrementAndGet();
            return "echo: " + text;
        }
    }

    private static final class FakeGeminiServer implements AutoCloseable {
        private static final String API_KEY = "local-contract-test-key";
        private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
        private final List<String> paths = new CopyOnWriteArrayList<>();
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        private FakeGeminiServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.setExecutor(executor);
            server.start();
        }

        private Client client() {
            return Client.builder()
                    .apiKey(API_KEY)
                    .vertexAI(false)
                    .httpOptions(HttpOptions.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .apiVersion("v1beta")
                            .timeout(10_000)
                            .build())
                    .build();
        }

        private void handle(HttpExchange exchange) throws IOException {
            JsonNode request = OBJECT_MAPPER.readTree(exchange.getRequestBody());
            requests.add(request);
            paths.add(exchange.getRequestURI().getPath());
            int requestNumber = requests.size();
            String response = switch (requestNumber) {
                case 1 -> firstFunctionCallResponse();
                case 2 -> secondFunctionCallResponse();
                default -> finalResponse();
            };
            boolean streaming = exchange.getRequestURI().getPath().contains("streamGenerateContent");

            if (streaming) {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
                exchange.sendResponseHeaders(200, 0);
                try (var body = exchange.getResponseBody()) {
                    body.write(("data: " + response + "\n\n").getBytes(StandardCharsets.UTF_8));
                    if (requestNumber >= 3) {
                        // Gemini may send the final token-usage chunk without candidates.
                        body.write(("data: " + usageOnlyResponse() + "\n\n")
                                .getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else {
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(bytes);
                }
            }
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }

        private static String firstFunctionCallResponse() throws IOException {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of("role", "model", "parts", List.of(
                                    Map.of(
                                            "functionCall", Map.of("name", "localEcho", "args", Map.of("text", "echo this")),
                                            "thoughtSignature", "AQID"),
                                    Map.of(
                                            "functionCall", Map.of("name", "localEcho", "args", Map.of("text", "echo that")),
                                            "thoughtSignature", "BwgJ"))),
                            "finishReason", "STOP",
                            "index", 0)),
                    "usageMetadata", Map.of("promptTokenCount", 5, "candidatesTokenCount", 2, "totalTokenCount", 7),
                    "modelVersion", "gemini-test"));
        }

        private static String secondFunctionCallResponse() throws IOException {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of("role", "model", "parts", List.of(Map.of(
                                    "functionCall", Map.of("name", "localEcho", "args", Map.of("text", "echo third")),
                                    "thoughtSignature", "CAkK"))),
                            "finishReason", "STOP",
                            "index", 0)),
                    "usageMetadata", Map.of("promptTokenCount", 7, "candidatesTokenCount", 2, "totalTokenCount", 9),
                    "modelVersion", "gemini-test"));
        }

        private static String finalResponse() throws IOException {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of("role", "model", "parts", List.of(
                                    Map.of("text", "private reasoning", "thought", true),
                                    Map.of("text", "Final "),
                                    Map.of("text", "answer"))),
                            "finishReason", "STOP",
                            "index", 0)),
                    "usageMetadata", Map.of("promptTokenCount", 10, "candidatesTokenCount", 4, "totalTokenCount", 14),
                    "modelVersion", "gemini-test"));
        }

        private static String usageOnlyResponse() throws IOException {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "usageMetadata", Map.of("promptTokenCount", 10, "candidatesTokenCount", 4, "totalTokenCount", 14),
                    "modelVersion", "gemini-test"));
        }
    }
}
