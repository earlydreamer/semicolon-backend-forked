package dukku.deposit.boundedContext.deposit.in;

import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.common.shared.payment.event.RefundRequestedEvent;
import dukku.deposit.boundedContext.deposit.app.DepositFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepositEventListenerHappyPathTest {

    @Mock
    private DepositFacade depositFacade;

    @InjectMocks
    private DepositEventListener listener;

    @Test
    @DisplayName("payment.refund-requested 이벤트를 받으면 예치금 환불 처리를 위임한다")
    void handleRefundRequested() {
        // given: 환불 이벤트에 필요한 식별자와 금액이 준비되어 있다.
        UUID refundUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        Long refundAmount = 8000L;
        Long depositRefundAmount = 3000L;

        RefundRequestedEvent event = new RefundRequestedEvent(
                refundUuid,
                paymentUuid,
                orderUuid,
                refundAmount,
                depositRefundAmount,
                userUuid,
                LocalDateTime.now()
        );

        // when: payment.refund-requested 핸들러를 실행한다.
        listener.handle(event);

        // then: 예치금 환불 파사드가 정확한 인자로 호출된다.
        verify(depositFacade).refundDeposit(userUuid, depositRefundAmount, orderUuid, paymentUuid, refundUuid);
    }

    @Test
    @DisplayName("payment.success 이벤트를 받으면 예치금 차감과 PG 입금 반영을 위임한다")
    void handlePaymentSuccess() {
        // given: 결제 성공 이벤트에 예치금 사용 금액과 PG 금액이 포함되어 있다.
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                paymentUuid,
                orderUuid,
                15000L,
                12000L,
                3000L,
                userUuid,
                LocalDateTime.now(),
                List.of(new PaymentSuccessEvent.ItemDepositUsage(orderItemUuid, 3000L))
        );

        // when: payment.success 핸들러를 실행한다.
        listener.handle(event);

        // then: 사용자 예치금 차감과 시스템 PG 입금 반영이 모두 호출된다.
        verify(depositFacade).deductDepositForPayment(
                userUuid,
                3000L,
                orderUuid,
                paymentUuid,
                event.itemDepositUsages()
        );
        verify(depositFacade).increaseSystemDepositForPg(orderUuid, 12000L);
    }
}