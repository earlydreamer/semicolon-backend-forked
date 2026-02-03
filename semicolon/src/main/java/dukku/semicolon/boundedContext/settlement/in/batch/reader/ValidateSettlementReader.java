package dukku.semicolon.boundedContext.settlement.in.batch.reader;

import dukku.semicolon.boundedContext.settlement.entity.Settlement;
import dukku.semicolon.boundedContext.settlement.entity.type.SettlementStatus;
import dukku.semicolon.boundedContext.settlement.in.batch.config.SettlementBatchProperties;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Step 2: 금액 검증 대상 Settlement Reader
 * - PENDING 상태의 Settlement 조회
 * - 정산 예약일이 현재 시간 이전인 건만 조회
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ValidateSettlementReader {

    private final EntityManagerFactory entityManagerFactory;
    private final SettlementBatchProperties batchProperties;

    @Bean
    public JpaPagingItemReader<Settlement> pendingSettlementForValidationReader() {
        String jpql = """
                SELECT s FROM Settlement s
                WHERE s.settlementStatus = :status
                AND s.settlementReservationDate <= :now
                ORDER BY s.settlementReservationDate ASC
                """;

        log.info("[Step 2] 금액 검증 대상 Settlement Reader 생성 - pageSize: {}", batchProperties.getPageSize());

        return new JpaPagingItemReaderBuilder<Settlement>()
                .name("pendingSettlementForValidationReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .parameterValues(Map.of(
                        "status", SettlementStatus.PENDING,
                        "now", LocalDateTime.now()
                ))
                .pageSize(batchProperties.getPageSize())
                .saveState(true)
                .build();
    }
}
