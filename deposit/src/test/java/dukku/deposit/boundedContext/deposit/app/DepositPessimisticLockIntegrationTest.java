package dukku.deposit.boundedContext.deposit.app;

import dukku.common.shared.deposit.type.DepositHistoryType;
import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import dukku.deposit.boundedContext.deposit.exception.NotEnoughDepositException;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:deposit_pessimistic_lock_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class DepositPessimisticLockIntegrationTest {

    @Autowired
    private DecreaseDepositUseCase decreaseDepositUseCase;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private DepositHistoryRepository depositHistoryRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        depositHistoryRepository.deleteAll();
        depositRepository.deleteAll();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("동일 사용자 예치금 동시 차감 시 비관적 락으로 직렬화되어 잔액 부족은 커스텀 예외로 처리된다")
    void concurrentDecreaseIsSerializedByPessimisticLock() throws Exception {
        // given: 사용자 예치금이 1,000원이고 동시에 700원 차감 요청 2개가 들어올 준비가 되어 있다.
        UUID userUuid = UUID.randomUUID();
        depositRepository.save(Deposit.builder()
                .userUuid(userUuid)
                .depositUuid(UUID.randomUUID())
                .balance(1000L)
                .version(0)
                .build());

        UUID orderItemA = UUID.randomUUID();
        UUID orderItemB = UUID.randomUUID();

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        Future<?> first = executorService.submit(() ->
                runDecreaseTask(userUuid, orderItemA, readyLatch, startLatch, successCount, failures));
        Future<?> second = executorService.submit(() ->
                runDecreaseTask(userUuid, orderItemB, readyLatch, startLatch, successCount, failures));

        readyLatch.await(3, TimeUnit.SECONDS);

        // when: 두 요청을 동시에 시작한다.
        startLatch.countDown();
        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);

        // then: 한 건만 성공하고, 실패는 잔액 부족 커스텀 예외로 수렴한다.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.get(0)).isInstanceOf(NotEnoughDepositException.class);

        Deposit after = depositRepository.findByUserUuid(userUuid).orElseThrow();
        assertThat(after.getBalance()).isEqualTo(300L);

        List<DepositHistory> histories = new ArrayList<>(
                depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(userUuid));
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getType()).isEqualTo(DepositHistoryType.USE);
        assertThat(histories.get(0).getAmount()).isEqualTo(700L);
    }

    private void runDecreaseTask(
            UUID userUuid,
            UUID orderItemUuid,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            AtomicInteger successCount,
            List<Throwable> failures) {
        try {
            readyLatch.countDown();
            startLatch.await(3, TimeUnit.SECONDS);
            decreaseDepositUseCase.decrease(userUuid, 700L, DepositHistoryType.USE, orderItemUuid);
            successCount.incrementAndGet();
        } catch (Throwable throwable) {
            failures.add(throwable);
        }
    }
}

