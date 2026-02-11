package dukku.coupon.boundedContext.deposit.app;

import dukku.common.global.config.QueryDslConfig;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositUsedEvent;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.coupon.boundedContext.deposit.entity.Deposit;
import dukku.coupon.boundedContext.deposit.entity.DepositHistory;
import dukku.coupon.boundedContext.deposit.entity.enums.DepositHistoryType;
import dukku.coupon.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.coupon.boundedContext.deposit.out.DepositRepository;
import dukku.coupon.global.SystemDepositInitData;
import dukku.coupon.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@Import({
        QueryDslConfig.class,
        DepositSupport.class,
        FindDepositUseCase.class,
        IncreaseDepositUseCase.class,
        DecreaseDepositUseCase.class,
        DeductDepositForPaymentUseCase.class
})
class DeductDepositForPaymentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private DeductDepositForPaymentUseCase deductDepositForPaymentUseCase;

    @Autowired
    private IncreaseDepositUseCase increaseDepositUseCase;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @Test
    @DisplayName("예치금 차감 시 사용자 잔액 감소 + 시스템 예치금 증가 + 히스토리 기록")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deductDepositAndRecordHistory() {
        try {
            UUID userUuid = UUID.randomUUID();
            UUID orderUuid = UUID.randomUUID();
            UUID itemUuid1 = UUID.randomUUID();
            UUID itemUuid2 = UUID.randomUUID();

            increaseDepositUseCase.increase(userUuid, 10000L, DepositHistoryType.CHARGE, null);

            List<PaymentSuccessEvent.ItemDepositUsage> usages = List.of(
                    new PaymentSuccessEvent.ItemDepositUsage(itemUuid1, 5000L),
                    new PaymentSuccessEvent.ItemDepositUsage(itemUuid2, 3000L));

            Long initialSystemBalance = depositRepository
                    .findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID)
                    .map(Deposit::getBalance)
                    .orElse(0L);

            deductDepositForPaymentUseCase.execute(userUuid, 8000L, orderUuid, usages);

            Deposit userDeposit = depositRepository.findByUserUuid(userUuid).orElseThrow();
            Deposit systemDeposit = depositRepository
                    .findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID)
                    .orElseThrow();

            assertThat(userDeposit.getBalance()).isEqualTo(2000L);
            assertThat(systemDeposit.getBalance()).isEqualTo(initialSystemBalance + 8000L);

            List<DepositHistory> userHistories = depositHistoryRepository
                    .findByUserUuidOrderByCreatedAtDesc(userUuid);
            List<DepositHistory> systemHistories = depositHistoryRepository
                    .findByUserUuidOrderByCreatedAtDesc(SystemDepositInitData.SYSTEM_USER_UUID);

            assertThat(userHistories).hasSize(3);
            assertThat(userHistories).anyMatch(history -> history.getType() == DepositHistoryType.USE
                    && itemUuid1.equals(history.getOrderItemUuid())
                    && history.getAmount() == 5000L);
            assertThat(userHistories).anyMatch(history -> history.getType() == DepositHistoryType.USE
                    && itemUuid2.equals(history.getOrderItemUuid())
                    && history.getAmount() == 3000L);

            assertThat(systemHistories.get(0).getType()).isEqualTo(DepositHistoryType.DEPOSIT_CHARGE);
            assertThat(systemHistories.get(0).getOrderItemUuid()).isEqualTo(orderUuid);
            assertThat(systemHistories.get(0).getAmount()).isEqualTo(8000L);

            verify(eventPublisher).publish(any(DepositUsedEvent.class));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
