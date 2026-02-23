package dukku.ai.app.usecase;

import java.util.UUID;

import dukku.ai.app.service.UserMemoryWriteService;
import dukku.ai.global.policy.AiPromptPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatUseCase {

    private final ChatClient chatClient;
    private final UserMemoryWriteService userMemoryWriteService;

    public ChatUseCase(ChatClient chatClient, UserMemoryWriteService userMemoryWriteService) {
        this.chatClient = chatClient;
        this.userMemoryWriteService = userMemoryWriteService;
    }

    public Flux<String> chat(String conversationId, UUID userUuid, String userMessage) {
        boolean shouldExtractMemory = userUuid != null && userMessage != null;

        Flux<String> responseFlux = chatClient.prompt()
                .system(s -> s.text(AiPromptPolicy.SYSTEM_PROMPT)
                        .param("user_uuid", userUuid != null ? userUuid.toString() : "알 수 없음"))
                .user(userMessage)
                .advisors(a -> {
                    a.param("chat_memory_conversation_id", conversationId);
                    if (userUuid != null) {
                        a.param("user_id", userUuid);
                    }
                    if (userMessage != null) {
                        a.param("user_message", userMessage);
                    }
                })
                .toolContext(java.util.Map.of("userId", userUuid != null ? userUuid.toString() : ""))
                .stream()
                .content();

        if (!shouldExtractMemory) {
            return responseFlux;
        }

        return Flux.defer(() -> {
            StringBuilder fullResponse = new StringBuilder();
            return responseFlux
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> {
                        String aiText = fullResponse.toString();
                        if (!aiText.isEmpty()) {
                            userMemoryWriteService.extractAndStoreMemories(userUuid, userMessage, aiText);
                            log.info("장기 기억 추출 트리거: userUuid={}", userUuid);
                        }
                    })
                    .doOnError(e -> log.warn("스트리밍 오류로 장기 기억 추출 건너뜀: {}", e.getMessage()));
        });
    }

}
