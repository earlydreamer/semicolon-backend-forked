package dukku.ai.app.usecase;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import dukku.ai.entity.AiUserMemory;
import dukku.ai.out.AiUserMemoryRepository;
import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import dukku.common.shared.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseMemoryUseCase {

    private final AiUserMemoryRepository aiUserMemoryRepository;
    private final EmbeddingModel embeddingModel;

    @Async
    public void storePurchaseMemory(UUID userUuid, List<OrderPaidEvent.PaidItem> items) {
        try {
            log.info("[PurchaseMemory] 구매 기억 저장 시작: userUuid={}, itemCount={}", userUuid, items.size());

            NumberFormat priceFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            String itemDescriptions = items.stream()
                    .map(item -> "%s(%s원)".formatted(item.productName(), priceFormat.format(item.productPrice())))
                    .collect(Collectors.joining(", "));

            String content = "%s 구매".formatted(itemDescriptions);
            float[] embedding = embeddingModel.embed(content);

            AiUserMemory memory = AiUserMemory.builder()
                    .userUuid(userUuid)
                    .memoryType(MemoryType.PURCHASE)
                    .subType(MemorySubType.SHOPPING)
                    .content(content)
                    .embedding(embedding)
                    .importanceScore(0.8)
                    .build();

            aiUserMemoryRepository.save(memory);

            log.info("[PurchaseMemory] 구매 기억 저장 완료: userUuid={}, content={}", userUuid, content);

        } catch (Exception e) {
            log.error("[PurchaseMemory] 구매 기억 저장 실패: userUuid={}, error={}", userUuid, e.getMessage(), e);
        }
    }
}
