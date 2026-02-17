package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.Refund;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleRefundFailureUseCaseTest {

    @Mock
    private PaymentSupport support;

    @InjectMocks
    private HandleRefundFailureUseCase useCase;

    @Test
    @DisplayName("DONE 상태 결제에서 환불 실패 시 PAYMENT_ROLLBACK_FAILED 이력이 기록된다")
    void handleRefundFailFromDoneCreatesRollbackFailedHistory() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 20000L, 5000L, 15000L, 0L,
                PaymentType.MIXED, "order-1");
        payment.approve("pg-key-1");

        Refund refund = payment.createRefund(5000L, 3000L, "idem-1");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(support.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when
        useCase.execute(refundUuid, paymentUuid, "rollback failed");

        // then
        verify(support).saveRefund(refund);
        verify(support).savePayment(payment);
        verify(support).createHistory(
                eq(payment),
                eq(PaymentHistoryType.PAYMENT_ROLLBACK_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("CANCELED 상태 결제에서 환불 실패 시 FULL_REFUND_FAILED 이력이 기록된다")
    void handleRefundFailFromCanceledCreatesFullFailedHistory() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 20000L, 15000L, 5000L, 0L,
                PaymentType.MIXED, "order-2");
        payment.approve("pg-key-2");
        payment.partialCancel(20000L, 5000L, 15000L);

        Refund refund = payment.createRefund(20000L, 15000L, "idem-2");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(support.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when
        useCase.execute(refundUuid, paymentUuid, "rollback failed");

        // then
        verify(support).saveRefund(refund);
        verify(support).savePayment(payment);
        verify(support).createHistory(
                eq(payment),
                eq(PaymentHistoryType.FULL_REFUND_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("PARTIAL_CANCELED 상태 결제에서 환불 실패 시 PARTIAL_REFUND_FAILED 이력이 기록된다")
    void handleRefundFailFromPartialCanceledCreatesPartialFailedHistory() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 20000L, 15000L, 5000L, 0L,
                PaymentType.MIXED, "order-3");
        payment.approve("pg-key-3");
        payment.partialCancel(5000L, 0L, 5000L);

        Refund refund = payment.createRefund(5000L, 5000L, "idem-3");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(support.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when
        useCase.execute(refundUuid, paymentUuid, "rollback failed");

        // then
        verify(support).saveRefund(refund);
        verify(support).savePayment(payment);
        verify(support).createHistory(
                eq(payment),
                eq(PaymentHistoryType.PARTIAL_REFUND_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("ROLLBACK_FAILED 상태에서는 payment 저장과 이력 생성이 생략된다")
    void skipRollbackDup() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 10000L, 0L, 10000L, 0L,
                PaymentType.NORMAL, "order-4");
        payment.rollbackFailedStatus();

        Refund refund = payment.createRefund(10000L, 0L, "idem-4");

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(support.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when
        useCase.execute(refundUuid, paymentUuid, "rollback failed");

        // then
        verify(support).saveRefund(refund);
        verify(support, never()).savePayment(any(Payment.class));
        verify(support, never()).createHistory(any(Payment.class), any(PaymentHistoryType.class),
                any(PaymentStatus.class), any(Long.class), any(Long.class));
    }
}
