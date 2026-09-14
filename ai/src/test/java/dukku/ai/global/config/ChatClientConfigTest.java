package dukku.ai.global.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dukku.ai.app.service.ProductRetrievalService;
import dukku.ai.app.service.UserMemoryReadService;
import dukku.ai.global.policy.AiGuardPolicy;
import dukku.ai.global.tool.CartHistoryTool;
import dukku.ai.global.tool.NotificationTool;
import dukku.ai.global.tool.PurchaseHistoryTool;
import dukku.ai.global.tool.RecommendationSaveTool;
import dukku.ai.global.tool.RecommendationTool;
import dukku.common.shared.ai.exception.AiQuestionTooLongException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatClientConfigTest {

    private final ChatModel model = mock(ChatModel.class);
    private final ProductRetrievalService productRetrievalService = mock(ProductRetrievalService.class);
    private final CartHistoryTool cartHistoryTool = mock(CartHistoryTool.class);
    private ChatMemory memory;
    private ChatClient client;
    private SingleConnectionDataSource database;

    @BeforeEach
    void setUp() {
        ChatClientConfig config = new ChatClientConfig();
        // 스키마 생성부터 검증 완료까지 같은 H2 연결을 유지한다.
        database = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID(), "sa", "", true);
        new ResourceDatabasePopulator(new ClassPathResource(
                "org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql")).execute(database);
        memory = config.chatMemory(JdbcChatMemoryRepository.builder()
                .jdbcTemplate(new JdbcTemplate(database)).build());
        when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        when(productRetrievalService.retrieve(anyString())).thenReturn("");
        client = config.chatClient(
                ChatClient.builder(model), ToolCallingAdvisor.builder(), memory,
                config.guardAdvisor(), config.loggingAdvisor(),
                config.memoryRetrievalAdvisor(mock(UserMemoryReadService.class)),
                config.toolAdvisor(), config.productRetrievalAdvisor(productRetrievalService),
                cartHistoryTool, mock(PurchaseHistoryTool.class), mock(RecommendationTool.class),
                mock(RecommendationSaveTool.class), mock(NotificationTool.class));
    }

    @AfterEach
    void tearDown() {
        if (database != null) {
            database.destroy();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void toolCallingPreservesHistoryAndStoresOnlyFinalExchange(boolean streaming) {
        String conversationId = "tool-conversation";
        memory.add(conversationId, List.of(new UserMessage("이전 질문"), new AssistantMessage("이전 답변")));
        when(cartHistoryTool.getCartProducts(any(ToolContext.class))).thenReturn(List.of("키보드"));

        ChatResponse toolRequest = new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "getCartProducts", "{}")))
                .build())));
        ChatResponse finalResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("키보드가 있어요."))));
        if (streaming) {
            when(model.stream(any(Prompt.class))).thenReturn(Flux.just(toolRequest)).thenReturn(Flux.just(finalResponse));
        } else {
            when(model.call(any(Prompt.class))).thenReturn(toolRequest, finalResponse);
        }

        var request = client.prompt().user("장바구니를 보여줘")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userId", "test-user"));
        String answer = streaming
                ? request.stream().content().collectList().map(parts -> String.join("", parts)).block(Duration.ofSeconds(10))
                : request.call().content();

        assertThat(answer).isEqualTo("키보드가 있어요.");
        var toolContext = ArgumentCaptor.forClass(ToolContext.class);
        verify(cartHistoryTool).getCartProducts(toolContext.capture());
        assertThat(toolContext.getValue().getContext()).containsEntry("userId", "test-user");
        verify(productRetrievalService).retrieve("장바구니를 보여줘");

        var prompts = ArgumentCaptor.forClass(Prompt.class);
        if (streaming) {
            verify(model, times(2)).stream(prompts.capture());
        } else {
            verify(model, times(2)).call(prompts.capture());
        }
        assertThat(prompts.getAllValues().get(1).getInstructions())
                .anyMatch(message -> message instanceof ToolResponseMessage)
                .anyMatch(message -> "이전 답변".equals(message.getText()));
        assertThat(memory.get(conversationId)).extracting(message -> message.getMessageType())
                .containsExactly(MessageType.USER, MessageType.ASSISTANT, MessageType.USER, MessageType.ASSISTANT);
        assertThat(memory.get(conversationId)).extracting(message -> message.getText())
                .containsExactly("이전 질문", "이전 답변", "장바구니를 보여줘", "키보드가 있어요.");
        assertThat(memory.get(conversationId))
                .noneMatch(message -> message instanceof AssistantMessage assistant && assistant.hasToolCalls());
    }

    @Test
    void guardRejectsInputBeforeMemorySearchOrModelCalls() {
        assertThatThrownBy(() -> client.prompt().user("가".repeat(AiGuardPolicy.MAX_INPUT_LENGTH + 1))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "rejected"))
                .call().content()).isInstanceOf(AiQuestionTooLongException.class);

        assertThat(memory.get("rejected")).isEmpty();
        verifyNoInteractions(productRetrievalService, cartHistoryTool);
        verify(model, never()).call(any(Prompt.class));
        verify(model, never()).stream(any(Prompt.class));
    }
}
