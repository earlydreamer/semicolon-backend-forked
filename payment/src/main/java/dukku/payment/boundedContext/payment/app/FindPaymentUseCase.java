package dukku.payment.boundedContext.payment.app;

import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.common.shared.payment.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 결제 내역 조회 UseCase
 *
 * <p>
 * 결제 UUID로 단건 조회하여 상세 정보 반환
 */
@Component
@RequiredArgsConstructor
public class FindPaymentUseCase {

    private final PaymentSupport support;

    @Transactional(readOnly = true)
    public Payment execute(UUID paymentUuid) {
        return support.findPaymentByUuid(paymentUuid);
    }

    @Transactional(readOnly = true)
    public Payment findByOrderUuid(UUID orderUuid) {
        List<Payment> payments = support.findPaymentsByOrderUuid(orderUuid);
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException();
        }
        return payments.get(0);
    }
}
