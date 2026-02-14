package dukku.deposit.boundedContext.deposit.app;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.deposit.type.DepositHistoryType;
import dukku.common.shared.payment.event.PaymentSuccessEvent;
import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
import dukku.deposit.global.SystemDepositInitData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:deposit_money_flow_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class DepositMoneyFlowIntegrationTest {

    @Autowired
    private DeductDepositForPaymentUseCase deductDepositForPaymentUseCase;

    @Autowired
    private IncreaseSystemDepositForPgUseCase increaseSystemDepositForPgUseCase;

    @Autowired
    private RefundDepositUseCase refundDepositUseCase;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        depositHistoryRepository.deleteAll();
        depositRepository.deleteAll();
    }

    @Test
    @DisplayName("예치금 차감 시 사용자 차감과 시스템 지갑 입금이 함께 반영된다")
    void deductUpdatesBalances() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();

        saveDeposit(userUuid, 10000L);
        saveDeposit(SystemDepositInitData.SYSTEM_USER_UUID, 1_000_000L);

        List<PaymentSuccessEvent.ItemDepositUsage> usages = List.of(
                new PaymentSuccessEvent.ItemDepositUsage(orderItemUuid, 3000L));

        deductDepositForPaymentUseCase.execute(userUuid, 3000L, orderUuid, paymentUuid, usages);

        assertThat(depositRepository.findByUserUuid(userUuid).orElseThrow().getBalance()).isEqualTo(7000L);
        assertThat(depositRepository.findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID).orElseThrow().getBalance())
                .isEqualTo(1_003_000L);

        List<DepositHistory> userHistories = depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(userUuid);
        assertThat(userHistories).isNotEmpty();
        assertThat(userHistories.get(0).getType()).isEqualTo(DepositHistoryType.USE);
        assertThat(userHistories.get(0).getAmount()).isEqualTo(3000L);
        assertThat(userHistories.get(0).getOrderItemUuid()).isEqualTo(orderItemUuid);

        List<DepositHistory> systemHistories = depositHistoryRepository
                .findByUserUuidOrderByCreatedAtDesc(SystemDepositInitData.SYSTEM_USER_UUID);
        assertThat(systemHistories).isNotEmpty();
        assertThat(systemHistories.get(0).getType()).isEqualTo(DepositHistoryType.DEPOSIT_CHARGE);
        assertThat(systemHistories.get(0).getAmount()).isEqualTo(3000L);
        assertThat(systemHistories.get(0).getOrderItemUuid()).isEqualTo(orderUuid);
    }

    @Test
    @DisplayName("PG 승인 금액은 시스템 지갑에 PG_CHARGE로 적립된다")
    void pgInflowUpdatesSystem() {
        UUID orderUuid = UUID.randomUUID();
        saveDeposit(SystemDepositInitData.SYSTEM_USER_UUID, 1_000_000L);

        increaseSystemDepositForPgUseCase.execute(orderUuid, 5000L);

        assertThat(depositRepository.findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID).orElseThrow().getBalance())
                .isEqualTo(1_005_000L);

        List<DepositHistory> systemHistories = depositHistoryRepository
                .findByUserUuidOrderByCreatedAtDesc(SystemDepositInitData.SYSTEM_USER_UUID);
        assertThat(systemHistories).isNotEmpty();
        assertThat(systemHistories.get(0).getType()).isEqualTo(DepositHistoryType.PG_CHARGE);
        assertThat(systemHistories.get(0).getAmount()).isEqualTo(5000L);
        assertThat(systemHistories.get(0).getOrderItemUuid()).isEqualTo(orderUuid);
    }

    @Test
    @DisplayName("시스템 지갑 잔액 부족이면 환불은 롤백되고 실패 이벤트만 발행된다")
    void refundFailRollsBack() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();

        saveDeposit(userUuid, 5000L);
        saveDeposit(SystemDepositInitData.SYSTEM_USER_UUID, 1000L);

        assertThatCode(() -> refundDepositUseCase.execute(userUuid, 3000L, orderUuid, paymentUuid, refundUuid))
                .doesNotThrowAnyException();

        assertThat(depositRepository.findByUserUuid(userUuid).orElseThrow().getBalance()).isEqualTo(5000L);
        assertThat(depositRepository.findByUserUuid(SystemDepositInitData.SYSTEM_USER_UUID).orElseThrow().getBalance())
                .isEqualTo(1000L);

        assertThat(depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(userUuid)).isEmpty();
        assertThat(depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(SystemDepositInitData.SYSTEM_USER_UUID))
                .isEmpty();

        org.mockito.ArgumentCaptor<DomainEvent> eventCaptor = org.mockito.ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(DepositRefundFailedEvent.class);
        DepositRefundFailedEvent event = (DepositRefundFailedEvent) eventCaptor.getValue();
        assertThat(event.refundId()).isEqualTo(refundUuid);
        assertThat(event.userUuid()).isEqualTo(userUuid);
        assertThat(event.amount()).isEqualTo(3000L);
    }

    private void saveDeposit(UUID userUuid, Long balance) {
        depositRepository.save(Deposit.builder()
                .userUuid(userUuid)
                .depositUuid(UUID.randomUUID())
                .balance(balance)
                .version(0)
                .build());
    }
}
