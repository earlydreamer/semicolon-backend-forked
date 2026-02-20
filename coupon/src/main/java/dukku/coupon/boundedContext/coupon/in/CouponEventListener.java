package dukku.coupon.boundedContext.coupon.in;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.coupon.boundedContext.coupon.app.command.CouponFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponEventListener {
    private final CouponFacade couponFacade;
    private final EventPublisher eventPublisher;

    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.success", groupId = "${spring.application.name}-group")
    public void handle(PaymentSuccessEvent event) {
        if (event.couponUuid() == null) {
            return;
        }

        try {
            couponFacade.useCoupon(event.userUuid(), event.couponUuid());
        } catch (CouponUseNotAllowedException e) {
            // 이미 사용된 쿠폰 (중복 수신) - 멱등 처리
            log.info("[쿠폰 사가] 이미 사용된 쿠폰입니다. 중복 이벤트로 판단하여 건너뜁니다. couponUuid={}", event.couponUuid());
        }
    }

    // TODO: 운영 대시보드에서 쿠폰 Saga 실패를 추적할 수 있도록 실패 이력 적재 필요 (DB 또는 별도 이벤트)
    @Recover
    public void recoverSuccess(Exception e, PaymentSuccessEvent event) {
        log.error("[쿠폰 사가] 재시도 후에도 쿠폰 적용에 실패했습니다. 롤백을 요청합니다. orderUuid={}, couponUuid={}",
                event.orderUuid(), event.couponUuid(), e);

        eventPublisher.publish(new PaymentRollbackRequestEvent(
                event.orderUuid(),
                "쿠폰 적용 실패로 인한 결제 롤백 요청: " + e.getMessage()));
    }

    @Retryable(backoff = @Backoff(delay = 1000))
    @KafkaListener(topics = "payment.failed", groupId = "${spring.application.name}-group")
    public void handle(PaymentFailedEvent event) {
        if (event.couponUuid() == null || event.userUuid() == null) {
            return;
        }

        couponFacade.rollbackCouponUseForPayment(event.userUuid(), event.couponUuid());
    }

    // TODO: 쿠폰 롤백 실패 시 운영 추적용 실패 이력 적재 필요 (DB 또는 별도 이벤트)
    @Recover
    public void recoverFailed(Exception e, PaymentFailedEvent event) {
        log.error(
                "[치명적 오류] 재시도 이후에도 쿠폰 롤백에 실패했습니다. 수동 조치가 필요합니다. orderUuid={}, couponUuid={}",
                event.orderUuid(), event.couponUuid(), e);
    }
}
