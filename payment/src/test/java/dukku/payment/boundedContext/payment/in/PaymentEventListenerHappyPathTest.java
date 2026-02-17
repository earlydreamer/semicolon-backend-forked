package dukku.payment.boundedContext.payment.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.common.shared.payment.type.RefundStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
/**
 * deposit.refunded / deposit.refund.failed 이벤트에 대한 PaymentEventListener 동작 테스트
 */
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
    @DisplayName("deposit.refunded 이벤트에서 PENDING refund가 COMPLETED로 전환되는지 검증")
    void handleDepositRefundedCompletesPendingRefund() {
        // given: PENDING 상태 환불과 결제 준비
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                20000L,
                15000L,
                5000L,
                0L,
                PaymentType.MIXED,
                "order-refunded-1");
        payment.approve("pg-key-refunded-1");

        Refund refund = payment.createRefund(7000L, 7000L, "idem-refunded-1");

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));

        // when: deposit.refunded 이벤트 수신
        listener.handle(new DepositRefundedEvent(
                refundUuid,
                paymentUuid,
                orderUuid,
                userUuid,
                7000L));

        // then: refund 상태 전환과 완료 이벤트 발행 확인
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(paymentSupport).saveRefund(refund);
        verify(eventPublisher).publish(any(RefundCompletedEvent.class));
    }

    @Test
    @DisplayName("deposit.refunded 이벤트에서 이미 COMPLETED 상태인 refund는 건너뛴다")
    void handleDepositRefundedSkipsWhenAlreadyCompleted() {
        // given: 이미 COMPLETED 상태인 refund 준비
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                20000L,
                15000L,
                5000L,
                0L,
                PaymentType.MIXED,
                "order-refunded-2");
        payment.approve("pg-key-refunded-2");

        Refund refund = payment.createRefund(7000L, 7000L, "idem-refunded-2");
        refund.complete();

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));

        // when: 동일 refund 대상의 deposit.refunded 이벤트 수신
        listener.handle(new DepositRefundedEvent(
                refundUuid,
                paymentUuid,
                orderUuid,
                userUuid,
                7000L));

        // then: 저장과 완료 이벤트 발행이 생략되는지 확인
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(paymentSupport, never()).saveRefund(refund);
        verify(eventPublisher, never()).publish(any(RefundCompletedEvent.class));
    }

    @Test
    @DisplayName("deposit.refund.failed 이벤트에서 DONE 상태 환불이 PAYMENT_ROLLBACK_FAILED 이력으로 전환되는지 검증")
    void handleRefundFailFromDoneCreatesRollbackFailedHistory() {
        // given: DONE 상태 결제와 환불 준비
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
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when: deposit.refund.failed 이벤트 수신
        listener.handle(event);

        // then: refund 취소와 payment rollback-failed 이력 기록 확인
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
    @DisplayName("deposit.refund.failed 이벤트에서 CANCELED 상태가 FULL_REFUND_FAILED 이력으로 전환되는지 검증")
    void handleRefundFailFromCanceledCreatesFullFailedHistory() {
        // given: CANCELED 상태 결제와 환불 준비
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                20000L,
                15000L,
                5000L,
                0L,
                PaymentType.MIXED,
                "order-2");
        payment.approve("pg-key-2");
        payment.partialCancel(20000L, 5000L, 15000L);

        Refund refund = payment.createRefund(20000L, 15000L, "idem-2");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid,
                orderUuid,
                paymentUuid,
                userUuid,
                15000L,
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when: deposit.refund.failed 이벤트 수신
        listener.handle(event);

        // then: FULL_REFUND_FAILED 이력 기록 확인
        verify(paymentSupport).saveRefund(refund);
        verify(paymentSupport).savePayment(payment);
        verify(paymentSupport).createHistory(
                eq(payment),
                eq(PaymentHistoryType.FULL_REFUND_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("deposit.refund.failed 이벤트에서 PARTIAL_CANCELED 상태가 PARTIAL_REFUND_FAILED 이력으로 전환되는지 검증")
    void handleRefundFailFromPartialCanceledCreatesPartialFailedHistory() {
        // given: PARTIAL_CANCELED 상태 결제와 환불 준비
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid,
                userUuid,
                20000L,
                15000L,
                5000L,
                0L,
                PaymentType.MIXED,
                "order-3");
        payment.approve("pg-key-3");
        payment.partialCancel(5000L, 0L, 5000L);

        Refund refund = payment.createRefund(5000L, 5000L, "idem-3");

        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid,
                orderUuid,
                paymentUuid,
                userUuid,
                5000L,
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when: deposit.refund.failed 이벤트 수신
        listener.handle(event);

        // then: PARTIAL_REFUND_FAILED 이력 기록 확인
        verify(paymentSupport).saveRefund(refund);
        verify(paymentSupport).savePayment(payment);
        verify(paymentSupport).createHistory(
                eq(payment),
                eq(PaymentHistoryType.PARTIAL_REFUND_FAILED),
                eq(originStatus),
                eq(originPg),
                eq(originDeposit));
    }

    @Test
    @DisplayName("ROLLBACK_FAILED 상태에서 rollback-failed 이벤트가 중복 처리되지 않는지 검증")
    void skipRollbackDup() {
        // given: rollback-failed 상태 결제와 환불 준비
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
                "order-4");
        payment.rollbackFailedStatus();

        Refund refund = payment.createRefund(10000L, 0L, "idem-4");

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid,
                orderUuid,
                paymentUuid,
                userUuid,
                10000L,
                DepositFailureCode.PERSISTENCE_ERROR,
                true,
                "rollback failed",
                LocalDateTime.now());

        when(paymentSupport.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));
        when(paymentSupport.findPaymentByUuidOptional(paymentUuid)).thenReturn(Optional.of(payment));

        // when: deposit.refund.failed 이벤트 수신
        listener.handle(event);

        // then: payment 저장과 이력 생성이 생략되고 refund만 저장되는지 확인
        verify(paymentSupport).saveRefund(refund);
        verify(paymentSupport, never()).savePayment(any(Payment.class));
        verify(paymentSupport, never()).createHistory(any(Payment.class), any(PaymentHistoryType.class),
                any(PaymentStatus.class), any(Long.class), any(Long.class));
    }
}