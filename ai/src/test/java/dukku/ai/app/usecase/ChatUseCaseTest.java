package dukku.ai.app.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import dukku.ai.app.service.UserMemoryWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

import reactor.core.publisher.Flux;

class ChatUseCaseTest {

    private static final String FAILURE_RESPONSE =
            "죄송합니다. 현재 AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해 주세요.";

    private ChatModel chatModel;
    private UserMemoryWriteService userMemoryWriteService;
    private ChatUseCase useCase;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        userMemoryWriteService = mock(UserMemoryWriteService.class);
        useCase = new ChatUseCase(ChatClient.builder(chatModel).build(), userMemoryWriteService);
    }

    @Test
    void extractsMemoryAfterSuccessfulStreamCompletion() {
        UUID userUuid = UUID.randomUUID();
        when(chatModel.stream(any(Prompt.class))).thenReturn(
                Flux.just(response("안녕 "), response("반가워")));

        List<String> chunks = useCase.chat("conversation-1", userUuid, "인사해 줘")
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).containsExactly("안녕 ", "반가워");
        verify(userMemoryWriteService).extractAndStoreMemories(userUuid, "인사해 줘", "안녕 반가워");
    }

    @Test
    void doesNotExtractMemoryWhenStreamFailsAfterPartialText() {
        UUID userUuid = UUID.randomUUID();
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.concat(
                Flux.just(response("일부 답변")),
                Flux.<ChatResponse>error(new IllegalStateException("provider failure"))));

        List<String> chunks = useCase.chat("conversation-2", userUuid, "질문")
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).containsExactly("일부 답변", FAILURE_RESPONSE);
        verifyNoInteractions(userMemoryWriteService);
    }

    @Test
    void doesNotExtractMemoryWhenSubscriberCancelsStream() {
        UUID userUuid = UUID.randomUUID();
        when(chatModel.stream(any(Prompt.class))).thenReturn(Flux.concat(
                Flux.just(response("일부 답변")),
                Flux.<ChatResponse>never()));

        List<String> chunks = useCase.chat("conversation-3", userUuid, "질문")
                .take(1)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(chunks).containsExactly("일부 답변");
        verifyNoInteractions(userMemoryWriteService);
    }

    private static ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }
}
