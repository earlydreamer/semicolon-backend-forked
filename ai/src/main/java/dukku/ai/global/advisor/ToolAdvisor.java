package dukku.ai.global.advisor;

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
