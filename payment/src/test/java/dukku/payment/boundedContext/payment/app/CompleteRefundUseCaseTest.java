package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.type.PaymentType;
import dukku.common.shared.payment.type.RefundStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteRefundUseCaseTest {

    @Mock
    private PaymentSupport support;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CompleteRefundUseCase useCase;

    @Test
    @DisplayName("PENDING refund가 COMPLETED로 전환되고 RefundCompletedEvent가 발행된다")
    void completesPendingRefund() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 20000L, 15000L, 5000L, 0L,
                PaymentType.MIXED, "order-refunded-1");
        payment.approve("pg-key-refunded-1");

        Refund refund = payment.createRefund(7000L, 7000L, "idem-refunded-1");

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));

        // when
        useCase.execute(refundUuid, paymentUuid);

        // then
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(support).saveRefund(refund);
        verify(eventPublisher).publish(any(RefundCompletedEvent.class));
    }

    @Test
    @DisplayName("이미 COMPLETED 상태인 refund는 저장과 이벤트 발행을 건너뛴다")
    void skipsWhenAlreadyCompleted() {
        // given
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        Payment payment = Payment.create(
                orderUuid, userUuid, 20000L, 15000L, 5000L, 0L,
                PaymentType.MIXED, "order-refunded-2");
        payment.approve("pg-key-refunded-2");

        Refund refund = payment.createRefund(7000L, 7000L, "idem-refunded-2");
        refund.complete();

        when(support.findRefundByUuid(refundUuid)).thenReturn(Optional.of(refund));

        // when
        useCase.execute(refundUuid, paymentUuid);

        // then
        assertThat(refund.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        verify(support, never()).saveRefund(refund);
        verify(eventPublisher, never()).publish(any(RefundCompletedEvent.class));
    }
}
