package dukku.payment.boundedContext.payment.app;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.coupon.out.CouponApiClient;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.common.shared.payment.dto.PaymentRequest;
import dukku.common.shared.payment.exception.AmountMismatchException;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import dukku.payment.boundedContext.payment.out.PaymentOrderItemRepository;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_request_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM=",
        "toss.api.secret-key=test_toss_secret_key"
})
@Transactional
class RequestPaymentUseCaseTest {

    @Autowired
    private RequestPaymentUseCase requestPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOrderItemRepository paymentOrderItemRepository;

    @MockitoBean
    private DepositApiClient depositApiClient;

    @MockitoBean
    private CouponApiClient couponApiClient;

    private UUID userUuid;

    @BeforeEach
    void setUpAuthentication() {
        userUuid = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(userUuid, "USER");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("쿠폰이 없으면 모든 상품 쿠폰 할인액은 0으로 저장된다")
    void noCouponDistributesZero() {
        when(depositApiClient.getBalance(any())).thenReturn(100_000L);

        PaymentRequest request = buildRequest(
                null,
                30_000L,
                0L,
                30_000L,
                5_000L,
                25_000L,
                List.of(
                        item(2, 20_000L),
                        item(1, 10_000L)
                ));

        requestPaymentUseCase.execute(request, "idem-no-coupon");

        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getPaymentCouponTotal()).isEqualTo(0L);

        List<PaymentOrderItem> orderItems = paymentOrderItemRepository.findByPaymentId(payment.getId());
        assertThat(orderItems).hasSize(2);
        assertThat(orderItems)
                .extracting(PaymentOrderItem::getPaymentCoupon)
                .containsExactly(0L, 0L);
    }

    @Test
    @DisplayName("쿠폰 할인액은 상품 가격 비례로 분배되고 합계가 유지된다")
    void distributesCouponProportionally() {
        UUID couponUuid = UUID.randomUUID();
        when(couponApiClient.getCouponInfo(couponUuid))
                .thenReturn(new CouponInternalResponse(3_000, 0, CouponStatus.ACTIVE));

        PaymentRequest request = buildRequest(
                couponUuid,
                30_000L,
                0L,
                27_000L,
                0L,
                27_000L,
                List.of(
                        item(2, 20_000L),
                        item(1, 10_000L)
                ));

        requestPaymentUseCase.execute(request, "idem-coupon-basic");

        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getPaymentCouponTotal()).isEqualTo(3_000L);

        Map<Integer, Long> couponByProduct = couponByProductId(payment);
        assertThat(couponByProduct.get(1)).isEqualTo(1_000L);
        assertThat(couponByProduct.get(2)).isEqualTo(2_000L);
        assertThat(couponByProduct.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("마지막 상품 오차 흡수로 초과가 발생하면 앞 상품으로 재분배한다")
    void rebalancesLastItemOverflow() {
        UUID couponUuid = UUID.randomUUID();
        when(couponApiClient.getCouponInfo(couponUuid))
                .thenReturn(new CouponInternalResponse(10_000, 0, CouponStatus.ACTIVE));

        PaymentRequest request = buildRequest(
                couponUuid,
                20_001L,
                0L,
                10_001L,
                0L,
                10_001L,
                List.of(
                        item(1, 10_000L),
                        item(2, 10_000L),
                        item(3, 1L)
                ));

        requestPaymentUseCase.execute(request, "idem-coupon-overflow");

        Payment payment = paymentRepository.findAll().get(0);
        List<PaymentOrderItem> orderItems = paymentOrderItemRepository.findByPaymentId(payment.getId());

        long totalCoupon = orderItems.stream().mapToLong(PaymentOrderItem::getPaymentCoupon).sum();
        assertThat(totalCoupon).isEqualTo(10_000L);
        assertThat(orderItems)
                .allSatisfy(item -> assertThat(item.getPaymentCoupon()).isLessThanOrEqualTo(item.getPrice()));

        Map<Integer, Long> couponByProduct = couponByProductId(payment);
        assertThat(couponByProduct.get(1)).isEqualTo(5_000L);
        assertThat(couponByProduct.get(2)).isEqualTo(4_999L);
        assertThat(couponByProduct.get(3)).isEqualTo(1L);
    }

    @Test
    @DisplayName("요청 itemsTotalAmount와 서버 계산 합계가 다르면 결제를 거절한다")
    void rejectsWhenItemsTotalMismatched() {
        PaymentRequest request = buildRequest(
                null,
                9_999L,
                0L,
                9_999L,
                0L,
                9_999L,
                List.of(item(1, 10_000L)));

        assertThatThrownBy(() -> requestPaymentUseCase.execute(request, "idem-total-mismatch"))
                .isInstanceOf(AmountMismatchException.class);

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 할인 총액이 상품 총액을 초과하면 결제를 거절한다")
    void rejectsWhenCouponExceedsItemsTotal() {
        UUID couponUuid = UUID.randomUUID();
        when(couponApiClient.getCouponInfo(couponUuid))
                .thenReturn(new CouponInternalResponse(20_000, 0, CouponStatus.ACTIVE));

        PaymentRequest request = buildRequest(
                couponUuid,
                10_000L,
                0L,
                10_000L,
                0L,
                10_000L,
                List.of(item(1, 10_000L)));

        assertThatThrownBy(() -> requestPaymentUseCase.execute(request, "idem-coupon-too-large"))
                .isInstanceOf(AmountMismatchException.class);

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("비활성 쿠폰 상태는 사용 불가 예외를 던진다")
    void rejectsWhenCouponIsInactive() {
        UUID couponUuid = UUID.randomUUID();
        when(couponApiClient.getCouponInfo(couponUuid))
                .thenReturn(new CouponInternalResponse(1_000, 0, CouponStatus.INACTIVE));

        PaymentRequest request = buildRequest(
                couponUuid,
                10_000L,
                0L,
                10_000L,
                0L,
                10_000L,
                List.of(item(1, 10_000L)));

        assertThatThrownBy(() -> requestPaymentUseCase.execute(request, "idem-coupon-inactive"))
                .isInstanceOf(CouponUseNotAllowedException.class);

        assertThat(paymentRepository.findAll()).isEmpty();
        verifyNoInteractions(depositApiClient);
    }

    private Map<Integer, Long> couponByProductId(Payment payment) {
        return paymentOrderItemRepository.findByPaymentId(payment.getId()).stream()
                .collect(Collectors.toMap(PaymentOrderItem::getProductId, PaymentOrderItem::getPaymentCoupon));
    }

    private PaymentRequest buildRequest(UUID couponUuid,
                                        Long itemsTotalAmount,
                                        Long couponDiscountAmount,
                                        Long finalPayAmount,
                                        Long depositUseAmount,
                                        Long pgPayAmount,
                                        List<PaymentRequest.PaymentRequestItem> items) {
        return PaymentRequest.builder()
                .orderUuid(UUID.randomUUID())
                .couponUuid(couponUuid)
                .orderName("request-payment-test-order")
                .amounts(PaymentRequest.Amounts.builder()
                        .itemsTotalAmount(itemsTotalAmount)
                        .couponDiscountAmount(couponDiscountAmount)
                        .finalPayAmount(finalPayAmount)
                        .depositUseAmount(depositUseAmount)
                        .pgPayAmount(pgPayAmount)
                        .build())
                .items(items)
                .build();
    }

    private PaymentRequest.PaymentRequestItem item(int productId, long price) {
        return PaymentRequest.PaymentRequestItem.builder()
                .orderItemUuid(UUID.randomUUID())
                .productId(productId)
                .productName("product-" + productId)
                .price(price)
                .sellerUuid(UUID.randomUUID())
                .build();
    }
}
