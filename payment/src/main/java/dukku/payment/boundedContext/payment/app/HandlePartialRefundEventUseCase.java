package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.exception.PaymentNotFoundException;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
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

        long totalRefundAmount = event.refundItems().stream()
                .mapToLong(PartialRefundRequestedEvent.RefundItemInfo::refundAmount)
                .sum();

        List<PaymentRefundRequest.RefundItemInfo> refundItemInfos = event.refundItems().stream()
                .map(item -> new PaymentRefundRequest.RefundItemInfo(item.orderItemUuid(), (long) item.refundAmount()))
                .toList();

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
}
