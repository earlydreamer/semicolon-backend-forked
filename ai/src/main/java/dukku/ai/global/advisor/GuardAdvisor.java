package dukku.ai.global.advisor;

import java.util.List;

import dukku.common.shared.ai.exception.AiForbiddenWordIncludedException;
import dukku.common.shared.ai.exception.AiQuestionTooLongException;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;

@RequiredArgsConstructor
public class GuardAdvisor implements BaseAdvisor {

    private final int maxLength;
    private final List<String> forbiddenWords;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        UserMessage userMessage = request.prompt().getUserMessage();
        if (userMessage == null) {
            return request;
        }

        String userText = userMessage.getText();
        if (userText == null) {
            return request;
        }

        if (userText.length() > maxLength) {
            throw new AiQuestionTooLongException(maxLength);
        }

        for (String word : forbiddenWords) {
            if (userText.contains(word)) {
                throw new AiForbiddenWordIncludedException(word);
            }
        }

        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "GuardAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
