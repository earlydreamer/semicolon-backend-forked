package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예치금 환불 완료 처리 UseCase
 *
 * <p>예치금 도메인에서 환불 완료 이벤트를 수신한 뒤
 * Refund 상태를 COMPLETED로 전이하고 후속 이벤트를 발행한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteRefundUseCase {

    private final PaymentSupport support;
    private final EventPublisher eventPublisher;

    /**
     * 환불 완료 처리
     *
     * @param refundUuid  환불 UUID
     * @param paymentUuid 결제 UUID (로깅용)
     */
    @Transactional
    public void execute(UUID refundUuid, UUID paymentUuid) {
        support.findRefundByUuid(refundUuid).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[결제 Saga 중복] 이미 완료된 환불 이벤트는 무시 refundUuid={}", refundUuid);
                return;
            }

            refund.complete();
            support.saveRefund(refund);

            Payment payment = refund.getPayment();
            eventPublisher.publish(new RefundCompletedEvent(
                    refund.getUuid(),
                    payment.getUuid(),
                    payment.getOrderUuid(),
                    refund.getRefundAmountTotal(),
                    refund.getRefundDepositTotal(),
                    payment.getUserUuid(),
                    LocalDateTime.now()));
        }, () -> log.warn("[결제 Saga 실패] deposit.refunded 조회 실패 refundUuid={}, paymentUuid={}",
                refundUuid, paymentUuid));
    }
}
