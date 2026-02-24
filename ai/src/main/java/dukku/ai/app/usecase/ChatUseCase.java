package dukku.ai.app.usecase;

import java.util.UUID;

import dukku.ai.app.service.UserMemoryWriteService;
import dukku.ai.global.policy.AiPromptPolicy;
import dukku.common.shared.ai.exception.AiGuardException;
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
                .system(AiPromptPolicy.SYSTEM_PROMPT)
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
            return responseFlux
                    .onErrorResume(e -> extractGuardException(e) != null, e -> {
                        AiGuardException guard = extractGuardException(e);
                        log.info("[Guard] 입력 검증 차단 → AI 응답으로 반환: {}", guard.getDetails());
                        return Flux.just(guard.getDetails());
                    });
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
                    .doOnError(e -> {
                        if (extractGuardException(e) == null) {
                            log.warn("스트리밍 오류로 장기 기억 추출 건너뜀: {}", e.getMessage());
                        }
                    })
                    .onErrorResume(e -> extractGuardException(e) != null, e -> {
                        AiGuardException guard = extractGuardException(e);
                        log.info("[Guard] 입력 검증 차단 → AI 응답으로 반환: {}", guard.getDetails());
                        return Flux.just(guard.getDetails());
                    });
        });
    }

        private static AiGuardException extractGuardException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof AiGuardException guard) {
                return guard;
            }
            current = current.getCause();
        }
        return null;
    }


}
