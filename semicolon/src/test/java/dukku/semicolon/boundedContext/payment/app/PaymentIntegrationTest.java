package dukku.semicolon.boundedContext.payment.app;

import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.PaymentType;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.semicolon.boundedContext.payment.out.PaymentRepository;
import dukku.semicolon.boundedContext.payment.out.TossPaymentClient;
import dukku.semicolon.shared.payment.dto.PaymentRefundRequest;
import dukku.semicolon.shared.payment.dto.PaymentRefundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class PaymentIntegrationTest {

        @Autowired
        private RefundPaymentUseCase refundPaymentUseCase;

        @Autowired
        private CompensatePaymentUseCase compensatePaymentUseCase;

        @Autowired
        private PaymentSupport paymentSupport;

        @MockitoSpyBean
        private PaymentRepository paymentRepository;

        @MockitoSpyBean
        private TossPaymentClient tossPaymentClient;

        private Payment testPayment;
        private final UUID userUuid = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                testPayment = Payment.builder()
                                .userUuid(userUuid)
                                .orderUuid(UUID.randomUUID())
                                .amount(20000L)
                                .amountPg(5000L)
                                .amountPgOrigin(5000L)
                                .paymentDeposit(15000L)
                                .paymentDepositOrigin(15000L)
                                .paymentCouponTotal(0L)
                                .paymentType(PaymentType.MIXED)
                                .paymentStatus(PaymentStatus.DONE)
                                .pgPaymentKey("test-payment-key")
                                .tossOrderId("test-order-id")
                                .refundTotal(0L)
                                .build();

                paymentSupport.savePayment(testPayment);
        }

        @Test
        @DisplayName("Full refund succeeds: PG canceled and deposit refunded")
        void fullRefundSuccessTest() {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("statusCode", 200);
                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(successResponse);

                PaymentRefundRequest request = PaymentRefundRequest.builder()
                                .paymentId(testPayment.getUuid())
                                .refundAmount(20000L)
                                .reason("customer_cancel")
                                .build();

                refundPaymentUseCase.execute(request, "idempotency-key");

                verify(tossPaymentClient).cancel(eq("test-payment-key"),
                                argThat(map -> map.get("cancelAmount").equals(5000L)));

                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
                assertThat(updatedPayment.getAmountPg()).isEqualTo(0L);
                assertThat(updatedPayment.getPaymentDeposit()).isEqualTo(0L);
                assertThat(updatedPayment.getRefundTotal()).isEqualTo(20000L);
        }

        @Test
        @DisplayName("PG cancel failure surfaces and marks rollback failed")
        void pgCancelFailureTest() {
                Map<String, Object> failureResponse = new HashMap<>();
                failureResponse.put("statusCode", 400);
                failureResponse.put("message", "PG cancel error");
                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(failureResponse);

                PaymentRefundRequest request = PaymentRefundRequest.builder()
                                .paymentId(testPayment.getUuid())
                                .refundAmount(20000L)
                                .reason("system_error")
                                .build();

                PaymentRefundResponse response = refundPaymentUseCase.execute(request, "idempotency-key");

                assertThat(response.isSuccess()).isFalse();
                assertThat(response.getCode()).isEqualTo("PG_CANCEL_FAILED");

                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.ROLLBACK_FAILED);
        }

        @Test
        @DisplayName("Partial refund flows allocate deposit first then PG")
        void partialRefundStepByStepTest() {
                Payment payment = Payment.builder()
                                .userUuid(userUuid)
                                .orderUuid(UUID.randomUUID())
                                .amount(20000L)
                                .amountPgOrigin(5000L)
                                .amountPg(5000L)
                                .paymentDepositOrigin(15000L)
                                .paymentDeposit(15000L)
                                .paymentCouponTotal(0L)
                                .paymentType(PaymentType.MIXED)
                                .paymentStatus(PaymentStatus.DONE)
                                .pgPaymentKey("partial-test-key")
                                .tossOrderId("toss-order-partial")
                                .refundTotal(0L)
                                .build();
                paymentSupport.savePayment(payment);

                // Step 1: refund 8,000 (all from deposit)
                PaymentRefundRequest req1 = PaymentRefundRequest.builder()
                                .paymentId(payment.getUuid())
                                .refundAmount(8000L)
                                .reason("partial-1")
                                .build();

                refundPaymentUseCase.execute(req1, "idempotency-1");

                Payment p1 = paymentSupport.findPaymentByUuid(payment.getUuid());
                assertThat(p1.getPaymentDeposit()).isEqualTo(7000L);
                assertThat(p1.getAmountPg()).isEqualTo(5000L);
                assertThat(p1.getRefundTotal()).isEqualTo(8000L);
                assertThat(p1.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
                verify(tossPaymentClient, never()).cancel(anyString(), anyMap());

                // Step 2: refund 10,000 (deposit 7,000 + PG 3,000)
                PaymentRefundRequest req2 = PaymentRefundRequest.builder()
                                .paymentId(payment.getUuid())
                                .refundAmount(10000L)
                                .reason("partial-2")
                                .build();

                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(Map.of("statusCode", 200));

                refundPaymentUseCase.execute(req2, "idempotency-2");

                Payment p2 = paymentSupport.findPaymentByUuid(payment.getUuid());
                assertThat(p2.getPaymentDeposit()).isEqualTo(0L);
                assertThat(p2.getAmountPg()).isEqualTo(2000L);
                assertThat(p2.getRefundTotal()).isEqualTo(18000L);
                assertThat(p2.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);

                verify(tossPaymentClient, times(1)).cancel(eq("partial-test-key"),
                                argThat(map -> map.get("cancelAmount").equals(3000L)));

                // Step 3: refund remaining 2,000 (PG only)
                PaymentRefundRequest req3 = PaymentRefundRequest.builder()
                                .paymentId(payment.getUuid())
                                .refundAmount(2000L)
                                .reason("partial-3")
                                .build();

                refundPaymentUseCase.execute(req3, "idempotency-3");

                Payment p3 = paymentSupport.findPaymentByUuid(payment.getUuid());
                assertThat(p3.getPaymentDeposit()).isEqualTo(0L);
                assertThat(p3.getAmountPg()).isEqualTo(0L);
                assertThat(p3.getRefundTotal()).isEqualTo(20000L);
                assertThat(p3.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);

                verify(tossPaymentClient, times(1)).cancel(eq("partial-test-key"),
                                argThat(map -> map.get("cancelAmount").equals(2000L)));
        }

        @Test
        @DisplayName("Compensation cancels PG only and does not re-credit deposit")
        void compensatePaymentSuccessTest() {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("statusCode", 200);
                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(successResponse);

                compensatePaymentUseCase.execute(testPayment.getOrderUuid(), "deposit deduction failed");

                verify(tossPaymentClient).cancel(eq("test-payment-key"),
                                argThat(map -> map.get("cancelAmount").equals(5000L)));

                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                assertThat(updatedPayment.getAmountPg()).isEqualTo(0L);
                assertThat(updatedPayment.getPaymentDeposit()).isEqualTo(0L);
                assertThat(updatedPayment.getRefundTotal()).isEqualTo(20000L);
        }

        @Test
        @DisplayName("Retryable PG cancel eventually succeeds")
        void retryVerificationTest() {
                PaymentRefundRequest request = PaymentRefundRequest.builder()
                                .paymentId(testPayment.getUuid())
                                .refundAmount(20000L)
                                .reason("retry-test")
                                .build();

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("statusCode", 200);

                when(tossPaymentClient.cancel(anyString(), anyMap()))
                                .thenThrow(new RuntimeException("Retry-1"))
                                .thenThrow(new RuntimeException("Retry-2"))
                                .thenReturn(successResponse);

                refundPaymentUseCase.execute(request, "retry-test-key");

                verify(tossPaymentClient, times(3)).cancel(eq("test-payment-key"), anyMap());

                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        }

        @Test
        @DisplayName("savePayment delegates to repository (smoke)")
        void dbRetryVerificationTest() {
                paymentSupport.savePayment(testPayment);

                verify(paymentRepository, atLeastOnce()).save(any());
        }

        @Test
        @DisplayName("Refund idempotency: duplicate key returns same result")
        void refundIdempotencyTest() {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("statusCode", 200);
                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(successResponse);

                PaymentRefundRequest request = PaymentRefundRequest.builder()
                                .paymentId(testPayment.getUuid())
                                .refundAmount(20000L)
                                .reason("idempotency-test")
                                .build();

                // 첫 번째 환불 요청
                refundPaymentUseCase.execute(request, "same-idempotency-key");

                // 두 번째 동일한 idempotencyKey로 환불 요청
                refundPaymentUseCase.execute(request, "same-idempotency-key");

                // PG 취소는 1번만 호출되어야 함
                verify(tossPaymentClient, times(1)).cancel(eq("test-payment-key"), anyMap());

                // 최종 상태 확인
                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
                assertThat(updatedPayment.getRefundTotal()).isEqualTo(20000L);
        }

        @Test
        @DisplayName("Compensation idempotency: duplicate execution skips second call")
        void compensationIdempotencyTest() {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("statusCode", 200);
                when(tossPaymentClient.cancel(anyString(), anyMap())).thenReturn(successResponse);

                // 첫 번째 보상 트랜잭션
                compensatePaymentUseCase.execute(testPayment.getOrderUuid(), "deposit deduction failed");

                // 두 번째 보상 트랜잭션 (동일 orderUuid)
                compensatePaymentUseCase.execute(testPayment.getOrderUuid(), "deposit deduction failed");

                // PG 취소는 1번만 호출되어야 함
                verify(tossPaymentClient, times(1)).cancel(eq("test-payment-key"), anyMap());

                // 최종 상태 확인
                Payment updatedPayment = paymentSupport.findPaymentByUuid(testPayment.getUuid());
                assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                assertThat(updatedPayment.getAmountPg()).isEqualTo(0L);
        }
}
