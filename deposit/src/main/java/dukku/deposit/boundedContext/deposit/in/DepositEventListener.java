package dukku.deposit.boundedContext.deposit.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundRequestedEvent;
import dukku.common.shared.settlement.event.SettlementDepositChargeRequestedEvent;
import dukku.common.shared.user.event.UserDepositInitializationFailedEvent;
import dukku.common.shared.user.event.UserDepositInitializedEvent;
import dukku.common.shared.user.event.UserJoinedEvent;
import dukku.common.shared.user.event.UserProductInitializationFailedEvent;
import dukku.deposit.boundedContext.deposit.app.ChargeDepositForSettlementUseCase;
import dukku.deposit.boundedContext.deposit.app.DepositFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositEventListener {

    private final DepositFacade depositFacade;
    private final EventPublisher eventPublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.success", groupId = "${spring.application.name}-group")
    public void handle(PaymentSuccessEvent event) {
        depositFacade.deductDepositForPayment(
                event.userUuid(),
                event.paymentDeposit(),
                event.orderUuid(),
                event.paymentUuid(),
                event.itemDepositUsages());
        depositFacade.increaseSystemDepositForPg(event.orderUuid(), event.pgAmount());
    }

    @KafkaListener(topics = "payment.refund-requested", groupId = "${spring.application.name}-group")
    public void handle(RefundRequestedEvent event) {
        depositFacade.refundDeposit(
                event.userUuid(),
                event.refundDepositAmount(),
                event.orderUuid(),
                event.paymentUuid(),
                event.refundUuid());
    }

    @Deprecated
    @KafkaListener(topics = "settlement.deposit-charge", groupId = "${spring.application.name}-group")
    public void handle(SettlementDepositChargeRequestedEvent command) {
        log.warn("[DEPRECATED] 이벤트 기반 정산 충전 요청을 수신했습니다. settlementUuid={}", command.settlementUuid());
        depositFacade.chargeDepositForSettlement(
                command.userUuid(),
                command.amount(),
                command.settlementUuid());
    }

    @KafkaListener(topics = "user.joined", groupId = "${spring.application.name}-group")
    public void handleUserJoined(String eventJson) {
        try {
            UserJoinedEvent event = objectMapper.readValue(eventJson, UserJoinedEvent.class);
            depositFacade.findDeposit(event.member().userUuid());
            eventPublisher.publish(new UserDepositInitializedEvent(event.member().userUuid()));
            log.info("[UserJoinedEvent] 예치금 계정 초기화 완료. userUuid={}", event.member().userUuid());
        } catch (Exception e) {
            publishDepositInitFailed(eventJson, e);
        }
    }

    @KafkaListener(topics = "user.product-initialization-failed", groupId = "${spring.application.name}-group")
    public void handleProductInitializationFailed(String eventJson) {
        try {
            UserProductInitializationFailedEvent event =
                    objectMapper.readValue(eventJson, UserProductInitializationFailedEvent.class);
            depositFacade.compensateUserRegistration(event.userUuid());
            log.info("[UserProductInitializationFailedEvent] 예치금 보상(삭제) 완료. userUuid={}", event.userUuid());
        } catch (Exception e) {
            log.error("[UserProductInitializationFailedEvent] 예치금 보상(삭제) 처리 실패", e);
        }
    }

    private void publishDepositInitFailed(String eventJson, Exception cause) {
        try {
            UserJoinedEvent event = objectMapper.readValue(eventJson, UserJoinedEvent.class);
            String reason = cause.getMessage() == null ? "예치금 계정 초기화 중 예외 발생" : cause.getMessage();
            eventPublisher.publish(new UserDepositInitializationFailedEvent(event.member().userUuid(), reason));
            log.error("[UserJoinedEvent] 예치금 계정 초기화 실패. userUuid={}", event.member().userUuid(), cause);
        } catch (Exception parseException) {
            log.error("[UserJoinedEvent] 실패 이벤트 발행을 위한 파싱에 실패했습니다.", parseException);
        }
    }
}
