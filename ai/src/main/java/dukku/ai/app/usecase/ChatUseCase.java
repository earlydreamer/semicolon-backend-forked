package dukku.ai.app.usecase;

import java.util.UUID;

import dukku.ai.global.policy.AiPromptPolicy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatUseCase {

    private final ChatClient chatClient;

    public ChatUseCase(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Flux<String> chat(String conversationId, UUID userUuid, String userMessage) {
        return chatClient.prompt()
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
    }

}
