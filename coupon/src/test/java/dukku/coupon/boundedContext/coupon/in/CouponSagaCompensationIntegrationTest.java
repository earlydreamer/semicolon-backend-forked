package dukku.coupon.boundedContext.coupon.in;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.coupon.type.CouponUserStatus;
import dukku.common.shared.order.event.PaymentRollbackRequestEvent;
import dukku.common.shared.payment.event.PaymentFailedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentFailureStage;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:coupon_saga_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
class CouponSagaCompensationIntegrationTest {

    @Autowired
    private CouponEventListener listener;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUserRepository couponUserRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        couponUserRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("payment.success 이후 payment.failed 보상 시 쿠폰 상태가 USED -> AVAILABLE로 복구된다")
    void sagaCompensationRestoresCouponStatus() {
        UUID userUuid = UUID.randomUUID();
        UUID couponUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();

        Coupon coupon = couponRepository.save(activeCoupon(couponUuid));
        couponUserRepository.save(CouponUser.create(userUuid, coupon));

        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                paymentUuid,
                orderUuid,
                12000L,
                9000L,
                3000L,
                userUuid,
                LocalDateTime.now(),
                List.of(),
                couponUuid);

        listener.handle(successEvent);

        CouponUser used = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid).orElseThrow();
        assertThat(used.getStatus()).isEqualTo(CouponUserStatus.USED);

        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                orderUuid,
                paymentUuid,
                userUuid,
                PaymentFailureStage.DEPOSIT_DEDUCTION,
                PaymentFailureCode.DEPOSIT_DEDUCTION_FAILED,
                false,
                "compensation",
                LocalDateTime.now(),
                couponUuid);

        listener.handle(failedEvent);

        CouponUser restored = couponUserRepository.findByUserUuidAndCoupon_Uuid(userUuid, couponUuid).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(CouponUserStatus.AVAILABLE);
    }

    @Test
    @DisplayName("recoverSuccess는 재시도 소진 후 payment.rollback 이벤트를 발행한다")
    void publishesRollbackEventWhenCouponApplyFails() {
        UUID orderUuid = UUID.randomUUID();

        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                UUID.randomUUID(),
                orderUuid,
                12000L,
                9000L,
                3000L,
                UUID.randomUUID(),
                LocalDateTime.now(),
                List.of(),
                UUID.randomUUID());

        listener.recoverSuccess(new RuntimeException("coupon apply fail"), successEvent);

        org.mockito.ArgumentCaptor<DomainEvent> eventCaptor =
                org.mockito.ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(PaymentRollbackRequestEvent.class);

        PaymentRollbackRequestEvent rollbackEvent = (PaymentRollbackRequestEvent) eventCaptor.getValue();
        assertThat(rollbackEvent.orderUuid()).isEqualTo(orderUuid);
    }

    private Coupon activeCoupon(UUID couponUuid) {
        return Coupon.builder()
                .uuid(couponUuid)
                .couponName("saga-coupon")
                .discountAmount(1000)
                .minimumOrderAmount(1000)
                .validFrom(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .totalQuantity(100)
                .issuedQuantity(1)
                .build();
    }
}
