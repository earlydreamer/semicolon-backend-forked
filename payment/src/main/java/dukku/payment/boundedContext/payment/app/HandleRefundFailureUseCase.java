package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.RefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 예치금 환불 실패 처리 UseCase
 *
 * <p>예치금 도메인에서 환불 실패 이벤트를 수신한 뒤
 * Refund를 취소하고 Payment를 ROLLBACK_FAILED로 전이한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandleRefundFailureUseCase {

    private final PaymentSupport support;

    /**
     * 환불 실패 처리
     *
     * @param refundUuid  환불 UUID
     * @param paymentUuid 결제 UUID
     * @param reason      실패 사유
     */
    @Transactional
    public void execute(UUID refundUuid, UUID paymentUuid, String reason) {
        // 환불 엔티티를 취소 상태로 전환
        support.findRefundByUuid(refundUuid).ifPresentOrElse(refund -> {
            if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
                log.warn("[결제 Saga 실패] 이미 완료된 환불 refundUuid={}", refundUuid);
                return;
            }

            refund.cancel();
            support.saveRefund(refund);
        }, () -> log.warn("[결제 Saga 실패] deposit.refund.failed 조회 실패 refundUuid={}, paymentUuid={}",
                refundUuid, paymentUuid));

        support.findPaymentByUuidOptional(paymentUuid).ifPresentOrElse(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.ROLLBACK_FAILED) {
                return;
            }

            PaymentStatus originStatus = payment.getPaymentStatus();
            Long originAmountPg = payment.getAmountPg();
            Long originDeposit = payment.getPaymentDeposit();
            PaymentHistoryType historyType = resolveRefundFailureHistoryType(originStatus);

            payment.rollbackFailedStatus();
            support.savePayment(payment);
            support.createHistory(payment, historyType,
                    originStatus, originAmountPg, originDeposit);

            log.error("[결제 Saga 실패] deposit.refund.failed 처리 paymentUuid={}, reason={}",
                    paymentUuid, reason);
        }, () -> log.warn("[결제 Saga 실패] payment 조회 실패 paymentUuid={}", paymentUuid));
    }

    /**
     * 환불 실패 전 상태를 기준으로 이력 타입 계산
     *
     * @param originStatus 환불 실패 직전 결제 상태
     * @return 실패 이력 타입
     */
    private PaymentHistoryType resolveRefundFailureHistoryType(PaymentStatus originStatus) {
        if (originStatus == PaymentStatus.CANCELED) {
            return PaymentHistoryType.FULL_REFUND_FAILED;
        }
        if (originStatus == PaymentStatus.PARTIAL_CANCELED) {
            return PaymentHistoryType.PARTIAL_REFUND_FAILED;
        }
        return PaymentHistoryType.PAYMENT_ROLLBACK_FAILED;
    }
}
