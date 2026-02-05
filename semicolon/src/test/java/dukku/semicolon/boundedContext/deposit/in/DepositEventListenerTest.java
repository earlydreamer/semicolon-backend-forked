package dukku.semicolon.boundedContext.deposit.in;

import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.semicolon.boundedContext.deposit.app.DepositFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositEventListenerTest {

    private DepositFacade depositFacade;
    private DepositEventListener depositEventListener;

    @BeforeEach
    void setUp() {
        depositFacade = mock(DepositFacade.class);
        depositEventListener = new DepositEventListener(depositFacade);
    }

    @Test
    @DisplayName("PG 금액이 있으면 시스템 예치금을 증가시킨다")
    void handlePaymentSuccessCreditsSystemDeposit() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        List<PaymentSuccessEvent.ItemDepositUsage> usages = List.of(
                new PaymentSuccessEvent.ItemDepositUsage(UUID.randomUUID(), 5000L)
        );

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderUuid,
                10000L,
                3000L,
                7000L,
                userUuid,
                LocalDateTime.now(),
                usages
        );

        depositEventListener.handle(event);

        verify(depositFacade).deductDepositForPayment(userUuid, 7000L, orderUuid, usages);
        verify(depositFacade).increaseSystemDepositForPg(orderUuid, 3000L);
    }

    @Test
    @DisplayName("PG 금액이 없으면 시스템 예치금 증가를 생략한다")
    void handlePaymentSuccessSkipsWhenPgAmountZero() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        List<PaymentSuccessEvent.ItemDepositUsage> usages = List.of();

        PaymentSuccessEvent event = new PaymentSuccessEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderUuid,
                7000L,
                0L,
                7000L,
                userUuid,
                LocalDateTime.now(),
                usages
        );

        depositEventListener.handle(event);

        verify(depositFacade).deductDepositForPayment(userUuid, 7000L, orderUuid, usages);
        verify(depositFacade).increaseSystemDepositForPg(orderUuid, 0L);
    }
}
