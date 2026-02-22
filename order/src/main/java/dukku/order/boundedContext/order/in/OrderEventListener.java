package dukku.order.boundedContext.order.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundFailedEvent;
import dukku.order.boundedContext.order.app.UpdateOrderRefundStatusUseCase;
import dukku.order.boundedContext.order.app.UpdateOrderStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * 결제 모듈 이벤트를 수신해 주문 상태를 갱신
 * 재시도와 보상 흐름을 동일 리스너에서 처리
 */
public class OrderEventListener {
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final UpdateOrderRefundStatusUseCase updateOrderRefundStatusUseCase;
    private final EventPublisher eventPublisher;

    // payment.refund-completed 이벤트 수신 후 주문 환불 상태 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.refund-completed", groupId = "${spring.application.name}-group")
    public void handle(RefundCompletedEvent event) {
        updateOrderRefundStatusUseCase.updateRefund(event.refundUuid(), event.orderUuid(), event.refundAmount(),
                event.refundedItemUuids());
    }

    // 환불 완료 이벤트 반영 실패 시 수동 확인이 필요하다는 로그 기록
    @Recover
    public void recoverRefund(Exception e, RefundCompletedEvent event) {
        log.error(
                "[CRITICAL] refund-completed 이벤트 반영 실패. 수동 조치가 필요합니다. orderUuid={}, refundUuid={}, refundAmount={}",
                event.orderUuid(), event.refundUuid(), event.refundAmount(), e);
    }

    // payment.refund.failed 이벤트 수신 시 반품 거절(실패) 보상 트랜잭션 수행
    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.refund.failed", groupId = "${spring.application.name}-group")
    public void handle(RefundFailedEvent event) {
        log.warn("부분 환불 실패 이벤트 수신, 보상 트랜잭션(반품 거절) 처리 시작. orderUuid={}", event.orderUuid());
        updateOrderRefundStatusUseCase.failRefund(event.orderUuid());
    }

    // 환불 실패 보상 트랜잭션 반영 실패 시 로그
    @Recover
    public void recoverRefundFailed(Exception e, RefundFailedEvent event) {
        log.error(
                "[CRITICAL] refund-failed 이벤트 반영 실패. 수동 조치가 필요합니다. orderUuid={}",
                event.orderUuid(), e);
    }

    // payment.success 이벤트 수신 후 주문 결제 성공 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.success", groupId = "${spring.application.name}-group")
    public void handle(PaymentSuccessEvent event) {
        updateOrderStatusUseCase.confirmPayment(event.orderUuid());
    }

    // payment.success 처리 실패 시 결제 롤백 요청 이벤트 발행
    @Recover
    public void recoverSuccess(Exception e, PaymentSuccessEvent event) {
        log.error(
                "[CRITICAL] payment.success 이벤트로 주문 상태 갱신 실패. 롤백을 트리거합니다. orderUuid={}, error={}",
                event.orderUuid(), e.getMessage());

        eventPublisher.publish(
                new PaymentRollbackRequestEvent(
                        event.orderUuid(),
                        "결제 성공 후 주문 처리 실패로 롤백 환불을 요청합니다."));
    }

    // payment.failed 이벤트 수신 후 주문 실패 상태 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.failed", groupId = "${spring.application.name}-group")
    public void handle(PaymentFailedEvent event) {
        updateOrderStatusUseCase.failPayment(event.orderUuid());
    }

    // payment.failed 처리 실패 시 수동 확인이 필요하다는 로그 기록
    @Recover
    public void recoverFail(Exception e, PaymentFailedEvent event) {
        log.error(
                "[CRITICAL] payment.failed 이벤트를 주문 상태에 반영하지 못했습니다. 수동 조치가 필요합니다. orderUuid={}",
                event.orderUuid(), e);
    }
}
