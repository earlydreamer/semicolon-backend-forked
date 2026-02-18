package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentHistory;
import dukku.payment.boundedContext.payment.out.PaymentHistoryRepository;
import dukku.payment.boundedContext.payment.out.PaymentOrderItemRepository;
import dukku.payment.boundedContext.payment.out.PaymentRepository;
import dukku.payment.boundedContext.payment.out.RefundItemRepository;
import dukku.payment.boundedContext.payment.out.RefundRepository;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * PG 환불 실패 응답 시 결제 이력 기록(PAYMENT_ROLLBACK_FAILED)을 검증한 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_refund_fail_history_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@Transactional
class RefundPaymentFailureHistoryIntegrationTest {

    @Autowired
    private RefundPaymentUseCase refundPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundItemRepository refundItemRepository;

    @Autowired
    private PaymentOrderItemRepository paymentOrderItemRepository;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        paymentHistoryRepository.deleteAll();
        refundItemRepository.deleteAll();
        refundRepository.deleteAll();
        paymentOrderItemRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("PG 환불 실패 시 전체 환불 요청은 PAYMENT_ROLLBACK_FAILED 이력이 기록된다")
    void fullRefundFailureWritesPaymentRollbackFailedHistory() {
        // given: 전체 환불 요청과 PG 취소 실패 응답 준비
        Payment payment = createDonePayment(
                20000L,
                5000L,
                15000L,
                PaymentType.MIXED,
                "pg-key-fail-full-1",
                "toss-order-fail-full-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentUuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(20000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-fail-full-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 500, "message", "pg fail"));

        // when: 환불 유스케이스 실행
        PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idem-fail-full-1");

        // then: 결제는 rollback-failed로 전이되고 PAYMENT_ROLLBACK_FAILED 이력이 남는다
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("PG_CANCEL_FAILED");

        Payment updated = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.ROLLBACK_FAILED);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updated.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PaymentHistoryType.PAYMENT_ROLLBACK_FAILED);
    }

    @Test
    @DisplayName("PG 환불 실패 시 부분 환불 요청은 PAYMENT_ROLLBACK_FAILED 이력이 기록된다")
    void partialRefundFailureWritesPaymentRollbackFailedHistory() {
        // given: 부분 환불 요청과 PG 취소 실패 응답 준비
        Payment payment = createDonePayment(
                20000L,
                5000L,
                15000L,
                PaymentType.MIXED,
                "pg-key-fail-partial-1",
                "toss-order-fail-partial-1");

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .paymentUuid(payment.getUuid())
                .orderUuid(payment.getOrderUuid())
                .refundAmount(7000L)
                .reason("customer_cancel_request")
                .build();

        when(tossPaymentClient.cancel(eq("pg-key-fail-partial-1"), anyMap()))
                .thenReturn(Map.of("statusCode", 500, "message", "pg fail"));

        // when: 환불 유스케이스 실행
        PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idem-fail-partial-1");

        // then: 결제는 rollback-failed로 전이되고 PAYMENT_ROLLBACK_FAILED 이력이 남는다
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("PG_CANCEL_FAILED");

        Payment updated = paymentRepository.findByUuid(payment.getUuid()).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.ROLLBACK_FAILED);

        List<PaymentHistory> histories = paymentHistoryRepository.findByPaymentId(updated.getId());
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(PaymentHistoryType.PAYMENT_ROLLBACK_FAILED);
    }

    private Payment createDonePayment(Long amount, Long depositAmount, Long pgAmount,
                                      PaymentType paymentType, String pgPaymentKey, String tossOrderId) {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount,
                depositAmount,
                pgAmount,
                0L,
                paymentType,
                tossOrderId);
        payment.approve(pgPaymentKey);
        return paymentRepository.save(payment);
    }
}
