package dukku.semicolon.boundedContext.settlement.in.batch.config;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.boundedContext.settlement.in.batch.listener.DepositChargeSkipListener;
import dukku.semicolon.boundedContext.settlement.in.batch.listener.SettlementBatchListener;
import dukku.semicolon.boundedContext.settlement.in.batch.processor.DepositChargeProcessor;
import dukku.semicolon.boundedContext.settlement.in.batch.processor.ValidateSettlementProcessor;
import dukku.semicolon.boundedContext.settlement.in.batch.writer.DepositChargeWriter;
import dukku.semicolon.boundedContext.settlement.in.batch.writer.ValidateSettlementWriter;
import dukku.semicolon.shared.settlement.exception.SettlementProcessingException;
import dukku.semicolon.shared.settlement.exception.SettlementValidationException;
import dukku.semicolon.boundedContext.settlement.in.batch.processor.RetrySettlementProcessor;
import dukku.semicolon.boundedContext.settlement.in.batch.writer.RetrySettlementWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 정산 배치 설정 (2-Step 구조, Step 1은 TODO)
 *
 * <pre>
 * Job: settlementJob
 *  ├─ [TODO] Step 1: createSettlementStep (정산 대상 생성)
 *  │   - Order BC API 호출 → 당일 확정된 OrderItem 조회 → Settlement 생성
 *  │   - Order BC API 구현 후 활성화
 *  │
 *  ├─ Step 1: validateSettlementStep (금액 검증)
 *  │   - PENDING Settlement 조회 → 금액 검증 → PROCESSING 상태
 *  │   - Settlement는 이벤트 리스너에서 생성됨 (OrderItemConfirmedEvent)
 *  │
 *  └─ Step 2: depositChargeStep (예치금 충전)
 *      - PROCESSING Settlement 조회 → Deposit API 동기 호출 → SUCCESS 상태
 *      - [TODO] Deposit BC API Client 구현 필요
 * </pre>
 *
 * [Step 분리 이유]
 * 1. 정산 대상 명확화 (Step 1에서 Settlement 생성 = 스냅샷 역할)
 * 2. 금액 검증과 실제 충전 분리 (책임 분리)
 * 3. 재시작 시 실패한 Step부터 재실행 가능
 * 4. 중복 정산 방지 (Idempotency)
 *
 * [TODO]
 * - Step 1 (CreateSettlement): Order BC API 구현 후 활성화
 * - Step 3 (DepositCharge): Deposit BC API Client 구현 필요
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SettlementBatchProperties batchProperties;

    // Listeners
    private final SettlementBatchListener batchListener;
    private final DepositChargeSkipListener depositChargeSkipListener;

    // TODO: Step 1 정산 대상 생성 (Order BC API 구현 후 활성화)
    // private final CreateSettlementReader createSettlementReader;
    // private final CreateSettlementProcessor createSettlementProcessor;
    // private final CreateSettlementWriter createSettlementWriter;

    // Step 1: 금액 검증
    private final JpaPagingItemReader<Settlement> pendingSettlementForValidationReader;
    private final ValidateSettlementProcessor validateSettlementProcessor;
    private final ValidateSettlementWriter validateSettlementWriter;

    // Step 2: 예치금 충전
    private final JpaPagingItemReader<Settlement> processingSettlementReader;
    private final DepositChargeProcessor depositChargeProcessor;
    private final DepositChargeWriter depositChargeWriter;

    // Retry: 재처리
    private final JpaPagingItemReader<Settlement> failedSettlementReader;
    private final RetrySettlementProcessor retrySettlementProcessor;
    private final RetrySettlementWriter retrySettlementWriter;


    /**
     * 정산 배치 Job
     * - [TODO] Step 1 (정산 대상 생성) → Step 2 (금액 검증) → Step 3 (예치금 충전)
     * - 현재: Step 1 (금액 검증) → Step 2 (예치금 충전)
     */
    @Bean
    public Job settlementJob() {
        log.info("정산 배치 Job 생성 (2-Step 구조, Step 1은 TODO)");
        return new JobBuilder("settlementJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchListener)
                // TODO: Order BC API 구현 후 활성화
                // .start(createSettlementStep())  // Step 1: 정산 대상 생성
                // .next(validateSettlementStep()) // Step 2: 금액 검증
                // .next(depositChargeStep())      // Step 3: 예치금 충전
                .start(validateSettlementStep()) // 현재 Step 1: 금액 검증
                .next(depositChargeStep())       // 현재 Step 2: 예치금 충전
                .build();
    }


    /**
     * 정산 재처리 배치 Job
     * - 1시간 전 실패한 정산 건들을 재처리
     * - Step 1 (재처리: FAILED → PENDING) → Step 2 (금액 검증) → Step 3 (예치금 충전)
     */
    @Bean
    public Job settlementRetryJob() {
        log.info("정산 재처리 배치 Job 생성 (3-Step 구조)");
        return new JobBuilder("settlementRetryJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(batchListener)
                .start(retrySettlementStep())    // Step 1: FAILED → PENDING
                .next(validateSettlementStep())  // Step 2: 금액 검증 (재사용)
                .next(depositChargeStep())       // Step 3: 예치금 충전 (재사용)
                .build();
    }


    /*
    ============================================================================
    TODO: Order BC API 구현 후 활성화
    ============================================================================

    Step 1: 정산 대상 생성
    - Order BC API 호출 → 당일(어제) 확정된 OrderItem 조회
    - Settlement 생성 (PENDING 상태)

    [TODO] Order BC에 다음 API 구현 필요:
    - GET /api/v1/internal/orders/items/confirmed?date={date}
    - Response: List<ConfirmedOrderItemDto>

    @Bean
    public Step createSettlementStep() {
        log.info("[Step 1] 정산 대상 생성 Step 생성 - chunkSize: {}", batchProperties.getChunkSize());

        return new StepBuilder("createSettlementStep", jobRepository)
                .<ConfirmedOrderItemDto, Settlement>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(createSettlementReader.createReader())
                .processor(createSettlementProcessor)
                .writer(createSettlementWriter)
                // Skip 정책
                .faultTolerant()
                .skip(SettlementProcessingException.class)
                .skipLimit(batchProperties.getSkipLimit())
                // Retry 정책
                .retry(DataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                // Listener
                .listener(batchListener)
                .build();
    }

    ============================================================================
    */


    /**
     * Step 1: 금액 검증 (현재)
     * - PENDING 상태의 Settlement 조회 (정산 예약일 <= 현재 시간)
     * - 금액 유효성 검증
     * - PENDING → PROCESSING 상태 전이
     *
     * [TODO] Order BC API 구현 후 Step 2로 변경됨
     */
    @Bean
    public Step validateSettlementStep() {
        log.info("[Step 1] 금액 검증 Step 생성 - chunkSize: {}, skipLimit: {}",
                batchProperties.getChunkSize(),
                batchProperties.getSkipLimit());

        return new StepBuilder("validateSettlementStep", jobRepository)
                .<Settlement, Settlement>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(pendingSettlementForValidationReader)
                .processor(validateSettlementProcessor)
                .writer(validateSettlementWriter)
                // Skip 정책
                .faultTolerant()
                .skip(SettlementValidationException.class)
                .skipLimit(batchProperties.getSkipLimit())
                // Retry 정책
                .retry(DataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                // Listener
                .listener(batchListener)
                .listener(depositChargeSkipListener)
                .build();
    }


    /**
     * Step 2: 예치금 충전 (현재)
     * - PROCESSING 상태의 Settlement 조회
     * - Deposit BC API Client를 통한 예치금 충전 (동기 방식)
     * - PROCESSING → SUCCESS 상태 전이
     *
     * [TODO] Deposit BC API Client 구현 필요
     * - DepositFacade 직접 참조 대신 API Client를 통해 호출
     * - Bounded Context 간 직접 참조 금지 원칙 준수
     *
     * [TODO] Order BC API 구현 후 Step 3으로 변경됨
     *
     * [Idempotency]
     * - 이미 SUCCESS/FAILED 상태인 건은 Processor에서 Skip (null 반환)
     */
    @Bean
    public Step depositChargeStep() {
        log.info("[Step 2] 예치금 충전 Step 생성 - chunkSize: {}, skipLimit: {}, retryLimit: {}",
                batchProperties.getChunkSize(),
                batchProperties.getSkipLimit(),
                batchProperties.getRetryLimit());

        return new StepBuilder("depositChargeStep", jobRepository)
                .<Settlement, Settlement>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(processingSettlementReader)
                .processor(depositChargeProcessor)
                .writer(depositChargeWriter)
                // Skip 정책
                .faultTolerant()
                .skip(SettlementValidationException.class)
                .skip(SettlementProcessingException.class)
                .skipLimit(batchProperties.getSkipLimit())
                // Retry 정책
                .retry(DataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                // Listener
                .listener(batchListener)
                .listener(depositChargeSkipListener)
                .build();
    }


    /**
     * Retry Step: 재처리
     * - FAILED 상태의 Settlement 조회
     * - FAILED → PENDING 상태 전이
     * - 이후 validateSettlementStep, depositChargeStep에서 정상 플로우 진행
     */
    @Bean
    public Step retrySettlementStep() {
        log.info("[Retry Step] 재처리 Step 생성 - chunkSize: {}, skipLimit: {}",
                batchProperties.getChunkSize(),
                batchProperties.getSkipLimit());

        return new StepBuilder("retrySettlementStep", jobRepository)
                .<Settlement, Settlement>chunk(batchProperties.getChunkSize(), transactionManager)
                .reader(failedSettlementReader)
                .processor(retrySettlementProcessor)
                .writer(retrySettlementWriter)
                // Skip 정책
                .faultTolerant()
                .skip(SettlementValidationException.class)
                .skipLimit(batchProperties.getSkipLimit())
                // Retry 정책
                .retry(DataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                // Listener
                .listener(batchListener)
                .build();
    }
}
