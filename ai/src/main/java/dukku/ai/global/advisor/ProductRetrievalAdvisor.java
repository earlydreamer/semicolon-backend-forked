package dukku.ai.global.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import dukku.ai.app.service.ProductRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ProductRetrievalAdvisor implements BaseAdvisor {

    private final ProductRetrievalService productRetrievalService;
    private final int order;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        UserMessage userMessage = request.prompt().getUserMessage();
        if (userMessage == null) {
            return request;
        }

        String context;
        try {
            context = productRetrievalService.retrieve(userMessage.getText());
        } catch (Exception e) {
            log.warn("[상품 검색 Advisor] 상품 검색 실패, 스킵: {}", e.getMessage());
            return request;
        }

        if (context.isEmpty()) {
            log.info("[상품 검색 Advisor] 관련 상품 없음 → 스킵");
            return request;
        }

        log.info("[상품 검색 Advisor] 상품 컨텍스트 주입 완료 ({}건)", context.lines().count() - 1);

        Prompt augmented = request.prompt().augmentSystemMessage(context);
        return request.mutate().prompt(augmented).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "ProductRetrievalAdvisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
