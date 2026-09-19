package dukku.ai.global.advisor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;

@Slf4j
@RequiredArgsConstructor
public class LoggingAdvisor implements BaseAdvisor {

    private final int order;

    public LoggingAdvisor() {
        this(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        UserMessage userMessage = request.prompt().getUserMessage();
        String conversationId = String.valueOf(
                request.context().getOrDefault("chat_memory_conversation_id", "N/A"));

        String userText = userMessage != null ? userMessage.getText() : null;
        log.info("[AI 요청] conversationId={}, userTextLength={}",
                conversationId,
                userText != null ? userText.length() : 0);
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.getResult() != null) {
            String content = chatResponse.getResult().getOutput().getText();
            if (content != null && !content.isEmpty()) {
                log.info("[AI 응답] contentLength={}", content.length());
            } else {
                log.info("[AI 응답 시작] (스트리밍 중이거나 텍스트 내용 없음)");
            }
        }

        if (chatResponse != null && chatResponse.getMetadata() != null
                && chatResponse.getMetadata().getUsage() != null) {
            var usage = chatResponse.getMetadata().getUsage();
            log.info("[AI 토큰] promptTokens={}, completionTokens={}, totalTokens={}",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }

        return response;
    }

    @Override
    public String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
