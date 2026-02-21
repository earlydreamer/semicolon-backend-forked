package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.exception.InvalidRefundAmountException;
import dukku.common.shared.payment.exception.PaymentNotFoundException;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandlePartialRefundEventUseCase {

    private final PaymentSupport support;
    private final RefundPaymentUseCase refundPaymentUseCase;

    @Transactional
    public void execute(PartialRefundRequestedEvent event) {
        log.info("부분 환불 이벤트 처리 시작 - orderUuid: {}, returnRequestUuid: {}", event.orderUuid(),
                event.returnRequestUuid());

        List<Payment> payments = support.findPaymentsByOrderUuid(event.orderUuid());
        Payment payment = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.DONE
                        || p.getPaymentStatus() == PaymentStatus.PARTIAL_CANCELED)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("환불 가능한 결제 내역이 없습니다. orderUuid: {}", event.orderUuid());
                    return new PaymentNotFoundException();
                });

        List<PaymentRefundRequest.RefundItemInfo> refundItemInfos = event.refundItems().stream()
                .map(item -> resolveRefundItem(payment, item))
                .toList();

        long totalRefundAmount = refundItemInfos.stream()
                .mapToLong(PaymentRefundRequest.RefundItemInfo::getRefundAmount)
                .sum();

        if (totalRefundAmount <= 0L) {
            throw InvalidRefundAmountException.invalid();
        }

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentUuid(payment.getUuid())
                .orderUuid(event.orderUuid())
                .refundAmount(totalRefundAmount)
                .reason("부분 반품 승인 환불 (Return: " + event.returnRequestUuid() + ")")
                .items(refundItemInfos)
                .build();

        // 멱등성 키로 returnRequestUuid 사용
        refundPaymentUseCase.execute(request, event.returnRequestUuid().toString());
    }

    private PaymentRefundRequest.RefundItemInfo resolveRefundItem(Payment payment,
            PartialRefundRequestedEvent.RefundItemInfo eventItem) {
        if (payment.getId() == null) {
            throw InvalidRefundAmountException.invalid();
        }

        PaymentOrderItem paymentOrderItem = support.findPaymentOrderItem(payment.getId(), eventItem.orderItemUuid())
                .orElseThrow(InvalidRefundAmountException::orderItemNotFound);

        long paymentCoupon = paymentOrderItem.getPaymentCoupon() == null ? 0L : paymentOrderItem.getPaymentCoupon();
        long netPaidAmount = paymentOrderItem.getPrice() - paymentCoupon;
        if (netPaidAmount < 0L) {
            throw InvalidRefundAmountException.paymentAmountCorrupted();
        }

        long alreadyRefunded = support.getRefundedAmountByPaymentOrderItem(paymentOrderItem.getId());
        long refundableAmount = netPaidAmount - alreadyRefunded;
        if (refundableAmount <= 0L) {
            throw InvalidRefundAmountException.itemExceedsAvailable(
                    (long) eventItem.refundAmount(),
                    Math.max(0L, refundableAmount));
        }

        if (eventItem.refundAmount() != refundableAmount) {
            log.info("부분 환불 금액 재계산 적용 - orderItemUuid={}, eventAmount={}, calculatedAmount={}",
                    eventItem.orderItemUuid(), eventItem.refundAmount(), refundableAmount);
        }

        return new PaymentRefundRequest.RefundItemInfo(eventItem.orderItemUuid(), refundableAmount);
    }
}
