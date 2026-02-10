package dukku.semicolon.boundedContext.deposit.app;

import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.semicolon.boundedContext.deposit.entity.Deposit;
import dukku.semicolon.boundedContext.deposit.entity.DepositHistory;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeductDepositForPaymentUseCaseIntegrationTest {

    @Autowired
    private DepositFacade depositFacade;

    @Autowired
    private DeductDepositForPaymentUseCase deductDepositForPaymentUseCase;

    @Autowired
    private FindDepositUseCase findDepositUseCase;

    @Autowired
    private DepositSupport depositSupport;

    @Test
    @DisplayName("예치금 차감 중 실패하면 부분 차감 없이 전체 롤백된다")
    void rollbackAllWhenDeductionFailsMidway() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();

        depositFacade.increaseDeposit(userUuid, 7000L, DepositHistoryType.CHARGE, UUID.randomUUID());

        List<PaymentSuccessEvent.ItemDepositUsage> itemDepositUsages = List.of(
                new PaymentSuccessEvent.ItemDepositUsage(UUID.randomUUID(), 3000L),
                new PaymentSuccessEvent.ItemDepositUsage(UUID.randomUUID(), 5000L));

        deductDepositForPaymentUseCase.execute(userUuid, 8000L, orderUuid, paymentUuid, itemDepositUsages);

        Deposit deposit = findDepositUseCase.findOrCreate(userUuid);
        List<DepositHistory> histories = depositSupport.findHistoriesByUserUuid(userUuid);

        assertThat(deposit.getBalance()).isEqualTo(7000L);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(DepositHistoryType.CHARGE);
    }
}
