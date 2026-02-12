package dukku.ai.app;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatUseCase {

    private final ChatClient chatClient;

    public ChatUseCase(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String conversationId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();
    }

}
