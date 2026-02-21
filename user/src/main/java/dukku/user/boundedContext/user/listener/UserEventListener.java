package dukku.user.boundedContext.user.listener;

import dukku.common.shared.user.event.UserDepositInitializationFailedEvent;
import dukku.common.shared.user.event.UserProductInitializationFailedEvent;
import dukku.user.boundedContext.user.app.user.CompensateUserRegistrationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final CompensateUserRegistrationUseCase compensateUserRegistrationUseCase;

    @KafkaListener(topics = "user.deposit-initialization-failed", groupId = "${spring.application.name}-group")
    public void handleDepositInitializationFailed(UserDepositInitializationFailedEvent event) {
        rollbackUser(event.userUuid(), "예치금 초기화 실패");
    }

    @KafkaListener(topics = "user.product-initialization-failed", groupId = "${spring.application.name}-group")
    public void handleProductInitializationFailed(UserProductInitializationFailedEvent event) {
        rollbackUser(event.userUuid(), "상품 도메인 유저 초기화 실패");
    }

    private void rollbackUser(UUID userUuid, String reason) {
        try {
            compensateUserRegistrationUseCase.hardDelete(userUuid);
            log.warn("[UserRegistrationCompensation] 유저 롤백(hard delete) 완료. userUuid={}, reason={}", userUuid, reason);
        } catch (Exception e) {
            log.error("[UserRegistrationCompensation] 유저 롤백(hard delete) 실패. userUuid={}, reason={}", userUuid, reason, e);
        }
    }
}
