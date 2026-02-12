package dukku.ai.app.usecase;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatUseCase {

    private final ChatClient chatClient;

    public ChatUseCase(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String conversationId, Long userId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> {
                    a.param("chat_memory_conversation_id", conversationId);
                    if (userId != null) {
                        a.param("user_id", userId);
                    }
                    if (userMessage != null) {
                        a.param("user_message", userMessage);
                    }
                })
                .call()
                .content();
    }

}
