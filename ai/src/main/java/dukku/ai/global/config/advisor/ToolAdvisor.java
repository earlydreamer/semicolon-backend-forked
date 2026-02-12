package dukku.ai.global.config.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ToolAdvisor implements BaseAdvisor {

    private static final String TOOL_PROMPT = """

            필요한 경우 제공된 Tool을 활용하여 작업을 수행하세요.
            각 단계에서 필요한 Tool을 순서대로 호출하고, 결과를 기반으로 다음 단계를 진행하세요.
            Tool 호출 결과를 사용자에게 자연스럽게 설명하세요.
            """;

    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        log.debug("[ToolAdvisor] Tool 컨텍스트 설정");
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null && chatResponse.getResult() != null
                && chatResponse.getResult().getMetadata() != null) {
            var metadata = chatResponse.getResult().getMetadata();
            var finishReason = metadata.getFinishReason();
            if ("TOOL_CALLS".equalsIgnoreCase(finishReason)) {
                log.info("[ToolAdvisor] Tool 호출 감지");
            }
        }
        return response;
    }

    @Override
    public String getName() {
        return "ToolAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
