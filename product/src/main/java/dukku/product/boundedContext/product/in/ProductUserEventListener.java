package dukku.product.boundedContext.product.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.user.event.UserDepositInitializedEvent;
import dukku.common.shared.user.event.UserProductInitializationFailedEvent;
import dukku.product.boundedContext.product.entity.ProductUser;
import dukku.product.boundedContext.product.out.ProductUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUserEventListener {

    private final ProductUserRepository productUserRepository;
    private final EventPublisher eventPublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "user.deposit-initialized", groupId = "${spring.application.name}-group")
    public void handleDepositInitialized(String eventJson) {
        try {
            UserDepositInitializedEvent event = objectMapper.readValue(eventJson, UserDepositInitializedEvent.class);
            UUID userUuid = event.userUuid();

            if (productUserRepository.existsById(userUuid)) {
                return;
            }

            productUserRepository.save(ProductUser.create(userUuid, fallbackNickname(userUuid)));
            log.info("[UserDepositInitializedEvent] 상품 도메인 유저 초기화 완료. userUuid={}", userUuid);
        } catch (Exception e) {
            publishProductInitFailed(eventJson, e);
        }
    }

    private void publishProductInitFailed(String eventJson, Exception cause) {
        try {
            UserDepositInitializedEvent event = objectMapper.readValue(eventJson, UserDepositInitializedEvent.class);
            String reason = cause.getMessage() == null ? "상품 도메인 유저 초기화 중 예외 발생" : cause.getMessage();
            eventPublisher.publish(new UserProductInitializationFailedEvent(event.userUuid(), reason));
            log.error("[UserDepositInitializedEvent] 상품 도메인 유저 초기화 실패. userUuid={}", event.userUuid(), cause);
        } catch (Exception parseException) {
            log.error("[UserDepositInitializedEvent] 실패 이벤트 발행을 위한 파싱에 실패했습니다.", parseException);
        }
    }

    private String fallbackNickname(UUID userUuid) {
        return "user-" + userUuid.toString().substring(0, 8);
    }
}
