package dukku.ai.global.config.advisor;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;

@RequiredArgsConstructor
public class GuardAdvisor implements BaseAdvisor {

    private static final int DEFAULT_MAX_LENGTH = 500;

    private final int maxLength;
    private final List<String> forbiddenWords;
    private final int order;

    public GuardAdvisor() {
        this(DEFAULT_MAX_LENGTH, List.of(), 0);
    }

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
            throw new IllegalArgumentException(
                    "질문이 너무 깁니다. 최대 " + maxLength + "자까지 입력 가능합니다.");
        }

        for (String word : forbiddenWords) {
            if (userText.contains(word)) {
                throw new IllegalArgumentException(
                        "금칙어가 포함되어 있습니다: " + word);
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
