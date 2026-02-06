package dukku.semicolon.boundedContext.deposit.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * DepositFacade 단위 테스트
 */
class DepositFacadeTest {

    private DeductDepositForPaymentUseCase deductDepositForPaymentUseCase;
    private DepositFacade depositFacade;

    @BeforeEach
    void setUp() {
        deductDepositForPaymentUseCase = mock(DeductDepositForPaymentUseCase.class);

        depositFacade = new DepositFacade(
                mock(FindDepositUseCase.class),
                mock(IncreaseDepositUseCase.class),
                mock(DecreaseDepositUseCase.class),
                mock(FindDepositHistoriesUseCase.class),
                deductDepositForPaymentUseCase,
                mock(RefundDepositUseCase.class),
                mock(ChargeDepositUseCase.class),
                mock(ChargeDepositForSettlementUseCase.class));
    }

    @Test
    @DisplayName("예치금 차감 테스트: UseCase로 위임되어야 한다")
    void deductDepositForPaymentTest() {
        // Given
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID itemUuid1 = UUID.randomUUID();
        UUID itemUuid2 = UUID.randomUUID();

        List<PaymentSuccessEvent.ItemDepositUsage> usages = Arrays.asList(
                new PaymentSuccessEvent.ItemDepositUsage(itemUuid1, 5000L),
                new PaymentSuccessEvent.ItemDepositUsage(itemUuid2, 3000L));

        // When
        depositFacade.deductDepositForPayment(userUuid, 8000L, orderUuid, paymentUuid, usages);

        // Then: UseCase가 호출되었는지 검증
        verify(deductDepositForPaymentUseCase).execute(userUuid, 8000L, orderUuid, paymentUuid, usages);
    }
}
