package dukku.payment.boundedContext.payment.in;

import dukku.common.shared.deposit.event.DepositDeductionFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.payment.boundedContext.payment.app.PaymentFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 도메인 이벤트 리스너
 *
 * <p>
 * 주문과 예치금 도메인에서 전달된 이벤트를 수신해 결제 상태를 전이
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PaymentFacade paymentFacade;

    /**
     * 예치금 차감 실패 시 결제 보상 트랜잭션 처리
     */
    @KafkaListener(topics = "deposit.deduction-failed", groupId = "${spring.application.name}-group")
    public void handle(DepositDeductionFailedEvent event) {
        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 주문 처리 실패 시 결제 롤백 이벤트 처리
     */
    @KafkaListener(topics = "payment.rollback", groupId = "${spring.application.name}-group")
    public void handle(PaymentRollbackRequestEvent event) {
        paymentFacade.compensatePayment(event.orderUuid(), event.reason());
    }

    /**
     * 예치금 환불 완료 이벤트 처리
     */
    @KafkaListener(topics = "deposit.refunded", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundedEvent event) {
        paymentFacade.completeRefund(event.refundUuid(), event.paymentUuid());
    }

    /**
     * 예치금 환불 실패 이벤트 처리
     */
    @KafkaListener(topics = "deposit.refund.failed", groupId = "${spring.application.name}-group")
    public void handle(DepositRefundFailedEvent event) {
        paymentFacade.handleRefundFailure(event);
    }

    /**
     * 부분 환불 요청 이벤트 처리 (반품 승인 후)
     */
    @KafkaListener(topics = "order.partial_refund_requested", groupId = "${spring.application.name}-group")
    public void handle(dukku.common.shared.order.event.PartialRefundRequestedEvent event) {
        paymentFacade.handlePartialRefund(event);
    }

    /**
     * 주문 상품 취소 이벤트 처리 (배송 전 취소)
     */
    @KafkaListener(topics = "order.item.canceled", groupId = "${spring.application.name}-group")
    public void handle(dukku.common.shared.order.event.OrderItemCanceledEvent event) {
        log.info("주문 상품 취소 이벤트 수신: orderItemUuid={}", event.orderItemUuid());
        paymentFacade.handleOrderItemCanceled(event);
    }
}
