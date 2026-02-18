package dukku.coupon.boundedContext.coupon.in;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import dukku.coupon.boundedContext.coupon.app.command.CouponFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponEventListenerHappyPathTest {

    @Mock
    private CouponFacade couponFacade;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CouponEventListener listener;

    @Test
    @DisplayName("payment.success + couponUuid가 있으면 쿠폰 사용을 위임한다")
    void useCouponOnPaymentSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID couponUuid = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                10000L,
                8000L,
                2000L,
                userUuid,
                LocalDateTime.now(),
                List.of(),
                couponUuid);

        listener.handle(event);

        verify(couponFacade).useCoupon(userUuid, couponUuid);
    }

    @Test
    @DisplayName("payment.success + couponUuid가 없으면 쿠폰 처리를 건너뛴다")
    void skipWhenCouponMissingOnPaymentSuccess() {
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                10000L,
                8000L,
                2000L,
                UUID.randomUUID(),
                LocalDateTime.now(),
                List.of(),
                null);

        listener.handle(event);

        verify(couponFacade, never()).useCoupon(any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("recoverSuccess는 재시도 소진 후 payment.rollback 이벤트를 발행한다")
    void publishRollbackRequestWhenCouponApplyFails() {
        UUID orderUuid = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                UUID.randomUUID(),
                orderUuid,
                10000L,
                8000L,
                2000L,
                UUID.randomUUID(),
                LocalDateTime.now(),
                List.of(),
                UUID.randomUUID());

        listener.recoverSuccess(new RuntimeException("coupon apply fail"), event);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentRollbackRequestEvent.class);

        PaymentRollbackRequestEvent rollbackEvent = (PaymentRollbackRequestEvent) eventCaptor.getValue();
        assertThat(rollbackEvent.orderUuid()).isEqualTo(orderUuid);
    }

    @Test
    @DisplayName("payment.failed + couponUuid가 있으면 쿠폰 롤백을 위임한다")
    void rollbackCouponOnPaymentFailed() {
        UUID userUuid = UUID.randomUUID();
        UUID couponUuid = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userUuid,
                PaymentFailureStage.DEPOSIT_DEDUCTION,
                PaymentFailureCode.DEPOSIT_DEDUCTION_FAILED,
                false,
                "rollback",
                LocalDateTime.now(),
                couponUuid);

        listener.handle(event);

        verify(couponFacade).rollbackCouponUseForPayment(userUuid, couponUuid);
    }

    @Test
    @DisplayName("payment.failed + couponUuid가 없으면 쿠폰 롤백을 건너뛴다")
    void skipRollbackWhenCouponMissingOnPaymentFailed() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentFailureStage.SYSTEM,
                PaymentFailureCode.UNKNOWN,
                true,
                "failed",
                LocalDateTime.now(),
                null);

        listener.handle(event);

        verify(couponFacade, never()).rollbackCouponUseForPayment(any(), any());
    }
}
