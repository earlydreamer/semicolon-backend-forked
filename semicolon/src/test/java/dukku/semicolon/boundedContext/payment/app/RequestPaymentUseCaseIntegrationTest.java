package dukku.semicolon.boundedContext.payment.app;

import dukku.common.global.UserUtil;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.semicolon.boundedContext.payment.entity.PaymentOrderItem;
import dukku.semicolon.boundedContext.payment.out.PaymentOrderItemRepository;
import dukku.semicolon.boundedContext.payment.out.PaymentRepository;
import dukku.semicolon.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.semicolon.shared.payment.dto.PaymentRequest;
import dukku.semicolon.shared.payment.dto.PaymentResponse;
import dukku.semicolon.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Import({ PaymentSupport.class, RequestPaymentUseCase.class })
class RequestPaymentUseCaseIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RequestPaymentUseCase requestPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOrderItemRepository paymentOrderItemRepository;

    @MockitoBean
    private DepositApiClient depositApiClient;

    @Test
    @DisplayName("결제 요청 시 payment_order_items에 예치금 분배 정보가 저장된다")
    void persistPaymentOrderItemsWithDepositAllocation() {
        UUID userUuid = UUID.randomUUID();

        try (MockedStatic<UserUtil> userUtil = org.mockito.Mockito.mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getUserId).thenReturn(userUuid);
            when(depositApiClient.getBalance(userUuid)).thenReturn(20000L);

            UUID orderUuid = UUID.randomUUID();
            UUID itemUuid1 = UUID.randomUUID();
            UUID itemUuid2 = UUID.randomUUID();

            PaymentRequest.PaymentRequestItem item1 = PaymentRequest.PaymentRequestItem.builder()
                    .orderItemUuid(itemUuid1)
                    .productId(2)
                    .productName("item-2")
                    .price(10000L)
                    .sellerUuid(UUID.randomUUID())
                    .paymentCoupon(0L)
                    .build();

            PaymentRequest.PaymentRequestItem item2 = PaymentRequest.PaymentRequestItem.builder()
                    .orderItemUuid(itemUuid2)
                    .productId(1)
                    .productName("item-1")
                    .price(10000L)
                    .sellerUuid(UUID.randomUUID())
                    .paymentCoupon(0L)
                    .build();

            PaymentRequest request = PaymentRequest.builder()
                    .orderUuid(orderUuid)
                    .orderName("test-order")
                    .amounts(PaymentRequest.Amounts.builder()
                            .itemsTotalAmount(20000L)
                            .couponDiscountAmount(0L)
                            .finalPayAmount(20000L)
                            .depositUseAmount(15000L)
                            .pgPayAmount(5000L)
                            .build())
                    .items(List.of(item1, item2))
                    .build();

            PaymentResponse response = requestPaymentUseCase.execute(request, "idempotency-key");

            Payment payment = paymentRepository.findByUuid(response.getData().getPaymentUuid()).orElseThrow();
            List<PaymentOrderItem> items = paymentOrderItemRepository.findByPaymentId(payment.getId());

            assertThat(items).hasSize(2);
            assertThat(items).anyMatch(item -> item.getOrderItemUuid().equals(itemUuid2)
                    && item.getPaymentDeposit() == 10000L);
            assertThat(items).anyMatch(item -> item.getOrderItemUuid().equals(itemUuid1)
                    && item.getPaymentDeposit() == 5000L);
        }
    }
}
