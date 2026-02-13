package dukku.ai.global.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import dukku.ai.app.service.MemoryRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MemoryRetrievalAdvisor implements BaseAdvisor {

    public static final String USER_ID_KEY = "user_id";

    private final MemoryRetrievalService memoryRetrievalService;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        Object userIdObj = request.context().get(USER_ID_KEY);
        if (userIdObj == null) {
            return request;
        }

        Long userId = Long.valueOf(userIdObj.toString());
        UserMessage userMessage = request.prompt().getUserMessage();
        if (userMessage == null) {
            return request;
        }

        String memoryContext = memoryRetrievalService.retrieveMemoryContext(
                userId, userMessage.getText());

        if (memoryContext.isEmpty()) {
            return request;
        }

        log.debug("장기 기억 주입: userId={}, context length={}", userId, memoryContext.length());

        Prompt augmented = request.prompt().augmentSystemMessage(memoryContext);
        return request.mutate().prompt(augmented).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "MemoryRetrievalAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
