package dukku.ai.global.advisor;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;

import dukku.ai.app.service.MemoryExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MemoryExtractionAdvisor implements BaseAdvisor {

    private final MemoryExtractionService memoryExtractionService;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        try {
            Object userUuidObj = response.context().get(MemoryRetrievalAdvisor.USER_ID_KEY);
            if (userUuidObj == null) {
                return response;
            }

            UUID userUuid = UUID.fromString(userUuidObj.toString());

            ChatResponse chatResponse = response.chatResponse();
            if (chatResponse == null || chatResponse.getResult() == null) {
                return response;
            }

            String aiText = chatResponse.getResult().getOutput().getText();
            Object userMessageObj = response.context().get("user_message");
            String userText = userMessageObj != null ? userMessageObj.toString() : "";

            if (!userText.isEmpty() && aiText != null && !aiText.isEmpty()) {
                memoryExtractionService.extractAndStoreMemories(userUuid, userText, aiText);
                log.debug("장기 기억 추출 트리거: userUuid={}", userUuid);
            }
        } catch (Exception e) {
            log.warn("장기 기억 추출 어드바이저 오류: {}", e.getMessage());
        }

        return response;
    }

    @Override
    public String getName() {
        return "MemoryExtractionAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
