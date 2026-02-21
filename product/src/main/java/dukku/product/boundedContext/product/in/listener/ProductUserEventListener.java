package dukku.product.boundedContext.product.in.listener;

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

    @KafkaListener(topics = "user.deposit-initialized", groupId = "${spring.application.name}-group")
    public void handleDepositInitialized(UserDepositInitializedEvent event) {
        try {
            UUID userUuid = event.userUuid();

            if (productUserRepository.existsById(userUuid)) {
                return;
            }

            productUserRepository.save(ProductUser.create(userUuid, fallbackNickname(userUuid)));
            log.info("[UserDepositInitializedEvent] 상품 도메인 유저 초기화 완료. userUuid={}", userUuid);
        } catch (Exception e) {
            publishProductInitFailed(event, e);
        }
    }

    private void publishProductInitFailed(UserDepositInitializedEvent event, Exception cause) {
        String reason = cause.getMessage() == null ? "상품 도메인 유저 초기화 중 예외 발생" : cause.getMessage();
        eventPublisher.publish(new UserProductInitializationFailedEvent(event.userUuid(), reason));
        log.error("[UserDepositInitializedEvent] 상품 도메인 유저 초기화 실패. userUuid={}", event.userUuid(), cause);
    }

    private String fallbackNickname(UUID userUuid) {
        return "user-" + userUuid.toString().substring(0, 8);
    }
}
