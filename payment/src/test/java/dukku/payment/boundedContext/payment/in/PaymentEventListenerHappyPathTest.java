package dukku.payment.boundedContext.payment.in;

import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.type.DepositFailureCode;
import dukku.payment.boundedContext.payment.app.PaymentFacade;
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
/**
 * PaymentEventListener가 파사드에 올바르게 위임하는지 검증
 */
class PaymentEventListenerHappyPathTest {

    @Mock
    private PaymentFacade paymentFacade;

    @InjectMocks
    private PaymentEventListener listener;

    @Test
    @DisplayName("deposit.refunded 이벤트를 파사드에 위임한다")
    void handleDepositRefundedDelegatesToFacade() {
        UUID refundUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        DepositRefundedEvent event = new DepositRefundedEvent(
                refundUuid, paymentUuid, orderUuid, userUuid, 7000L);

        listener.handle(event);

        verify(paymentFacade).completeRefund(refundUuid, paymentUuid);
    }

    @Test
    @DisplayName("deposit.refund.failed 이벤트를 파사드에 위임한다")
    void handleDepositRefundFailedDelegatesToFacade() {
        UUID refundUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        DepositRefundFailedEvent event = new DepositRefundFailedEvent(
                refundUuid, orderUuid, paymentUuid, userUuid,
                3000L, DepositFailureCode.PERSISTENCE_ERROR,
                true, "rollback failed", LocalDateTime.now());

        listener.handle(event);

        verify(paymentFacade).handleRefundFailure(event);
    }
}
