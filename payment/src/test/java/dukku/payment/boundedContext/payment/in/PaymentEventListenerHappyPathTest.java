package dukku.payment.boundedContext.payment.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.app.PaymentFacade;
import dukku.payment.boundedContext.payment.app.PaymentSupport;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerHappyPathTest {

    @Mock
    private PaymentFacade paymentFacade;

    @Mock
    private PaymentSupport paymentSupport;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentEventListener listener;

    @Test
    @DisplayName("deposit.refund.failed 이벤트를 받으면 refund를 취소하고 payment를 ROLLBACK_FAILED로 전이한다")
    void handleRefundFailFlow() {
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                20000L,
                5000L,
                15000L,
                0L,
                PaymentType.MIXED,
                "order-1");
        payment.approve("pg-key-1");

        Refund refund = payment.createRefund(5000L, 3000L, "idem-1");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid,
                orderUuid,
                paymentUuid,
                userUuid,
                3000L,
                dukku.common.shared.deposit.type.DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        listener.handle(event);

        verify(paymentSupport).saveRefund(refund);
        verify(paymentSupport).savePayment(payment);
        verify(paymentSupport).createHistory(
                eq(payment),
                eq(PaymentHistoryType.PAYMENT_ROLLBACK_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("이미 ROLLBACK_FAILED 상태면 payment 이력은 중복 반영하지 않는다")
    void skipRollbackDup() {
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                10000L,
                0L,
                10000L,
                0L,
                PaymentType.NORMAL,
                "order-2");
        payment.rollbackFailedStatus();

        Refund refund = payment.createRefund(10000L, 0L, "idem-2");

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid,
                orderUuid,
                paymentUuid,
                userUuid,
                10000L,
                dukku.common.shared.deposit.type.DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        listener.handle(event);

        verify(paymentSupport).saveRefund(refund);
        verify(paymentSupport, never()).savePayment(any(Payment.class));
        verify(paymentSupport, never()).createHistory(any(Payment.class), any(PaymentHistoryType.class),
                any(PaymentStatus.class), any(Long.class), any(Long.class));
    }
}
