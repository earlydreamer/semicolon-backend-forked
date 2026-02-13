package dukku.deposit.boundedContext.deposit.in;

import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.deposit.boundedContext.deposit.app.DepositFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DepositEventListenerHappyPathTest {

    @Mock
    private DepositFacade depositFacade;

    @InjectMocks
    private DepositEventListener listener;

    @Test
    @DisplayName("payment.refund-completed 이벤트를 받으면 예치금 환불 처리 파사드가 호출된다")
    void handlesRefundCompletedEventByCallingRefundDeposit() {
        // given: paymentId/order/payment의 환불 이벤트가 수신될 준비가 되어 있다.
        UUID refundUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();
        Long refundAmount = 8000L;
        Long depositRefundAmount = 3000L;

        RefundCompletedEvent event = new RefundCompletedEvent(
                refundUuid,
                paymentUuid,
                orderUuid,
                refundAmount,
                depositRefundAmount,
                userUuid,
                LocalDateTime.now()
        );

        // when: payment.refund-completed 토픽을 수신했을 때의 핸들러를 실행한다.
        listener.handle(event);

        // then: paymentId를 기준으로 환불 처리 파사드가 정확한 인자로 호출된다.
        verify(depositFacade).refundDeposit(userUuid, depositRefundAmount, orderUuid, paymentUuid, refundUuid);
    }
}