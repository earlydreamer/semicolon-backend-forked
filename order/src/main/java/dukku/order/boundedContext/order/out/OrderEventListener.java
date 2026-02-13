package dukku.order.boundedContext.order.out;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.order.boundedContext.order.app.UpdateOrderRefundStatusUseCase;
import dukku.order.boundedContext.order.app.UpdateOrderStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 * 결제 모듈 이벤트 수신으로 주문 상태를 동기화
 * 재시도/보상 라우팅은 동일 리스너에서 처리
 */
public class OrderEventListener {
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final UpdateOrderRefundStatusUseCase updateOrderRefundStatusUseCase;
    private final EventPublisher eventPublisher;

    // payment.refund-completed: 환불 완료 이벤트 수신 시 주문 환불액/상태 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @org.springframework.kafka.annotation.KafkaListener(topics = "payment.refund-completed", groupId = "${spring.application.name}-group")
    public void handle(RefundCompletedEvent event) {
        updateOrderRefundStatusUseCase.updateRefund(event.orderUuid(), event.refundAmount());
    }

    // 환불 완료 이벤트 반영 실패 시 수동 개입 필요 로그
    @Recover
    public void recoverRefund(Exception e, RefundCompletedEvent event) {
        log.error("[CRITICAL] Failed to apply refund-completed event. manual action required. orderUuid={}, refundId={}, refundAmount={}",
                event.orderUuid(), event.refundId(), event.refundAmount(), e);
    }

    // payment.success: 결제 완료 이벤트 수신 시 주문 결제 성공 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @org.springframework.kafka.annotation.KafkaListener(topics = "payment.success", groupId = "${spring.application.name}-group")
    public void handle(PaymentSuccessEvent event) {
        updateOrderStatusUseCase.confirmPayment(event.orderUuid());
    }

    // payment.success 처리 실패 시 결제 롤백 요청 이벤트 발행
    @Recover
    public void recoverSuccess(Exception e, PaymentSuccessEvent event) {
        log.error("[CRITICAL] Payment success event failed to update order. Triggering rollback. orderUuid={}, error={}",
                event.orderUuid(), e.getMessage());

        eventPublisher.publish(
                new PaymentRollbackRequestEvent(
                        event.orderUuid(),
                        "Order processing failed after payment success; trigger rollback refund"));
    }

    // payment.failed: 결제 실패 이벤트 수신 시 주문 실패 상태 반영
    @Retryable(backoff = @Backoff(delay = 1000))
    @org.springframework.kafka.annotation.KafkaListener(topics = "payment.failed", groupId = "${spring.application.name}-group")
    public void handle(PaymentFailedEvent event) {
        updateOrderStatusUseCase.failPayment(event.orderUuid());
    }

    // payment.failed 처리 실패 시 수동 개입 필요 로그
    @Recover
    public void recoverFail(Exception e, PaymentFailedEvent event) {
        log.error("[CRITICAL] Payment failed event could not be persisted to order. manual action required. orderUuid={}",
                event.orderUuid(), e);
    }
}
