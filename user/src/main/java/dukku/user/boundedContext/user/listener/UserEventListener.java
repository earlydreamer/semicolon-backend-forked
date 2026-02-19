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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "user.deposit-initialization-failed", groupId = "${spring.application.name}-group")
    public void handleDepositInitializationFailed(String eventJson) {
        try {
            UserDepositInitializationFailedEvent event =
                    objectMapper.readValue(eventJson, UserDepositInitializationFailedEvent.class);
            rollbackUser(event.userUuid(), "예치금 초기화 실패");
        } catch (Exception e) {
            log.error("[UserDepositInitializationFailedEvent] 이벤트 처리 실패", e);
        }
    }

    @KafkaListener(topics = "user.product-initialization-failed", groupId = "${spring.application.name}-group")
    public void handleProductInitializationFailed(String eventJson) {
        try {
            UserProductInitializationFailedEvent event =
                    objectMapper.readValue(eventJson, UserProductInitializationFailedEvent.class);
            rollbackUser(event.userUuid(), "상품 도메인 유저 초기화 실패");
        } catch (Exception e) {
            log.error("[UserProductInitializationFailedEvent] 이벤트 처리 실패", e);
        }
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
