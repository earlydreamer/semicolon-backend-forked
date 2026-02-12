package dukku.ai.app;

import dukku.ai.app.usecase.ChatUseCase;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChatFacade {

    private final ChatUseCase chatUseCase;

    public ChatFacade(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.conversationId());
        String reply = chatUseCase.chat(conversationId, request.userId(), request.message());
        return new ChatResponse(conversationId, reply);
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }

    public record ChatRequest(
            String conversationId,
            Long userId,
            String message
    ) {
    }

    public record ChatResponse(
            String conversationId,
            String reply
    ) {
    }
}
