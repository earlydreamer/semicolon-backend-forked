package dukku.ai.global.config.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import dukku.ai.app.service.DocumentRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DocumentRetrievalAdvisor implements BaseAdvisor {

    private final DocumentRetrievalService documentRetrievalUseCase;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        UserMessage userMessage = request.prompt().getUserMessage();
        if (userMessage == null) {
            return request;
        }

        String context = documentRetrievalUseCase.retrieve(userMessage.getText());

        if (context.isEmpty()) {
            return request;
        }

        log.debug("[DocumentRetrievalAdvisor] 문서 컨텍스트 주입, length={}", context.length());

        Prompt augmented = request.prompt().augmentSystemMessage(context);
        return request.mutate().prompt(augmented).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "DocumentRetrievalAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
