package dukku.settlement.boundedContext.settlement.app;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementMetrics {

    private final MeterRegistry meterRegistry;

    private Counter createdCounter;
    private Counter amountCounter;
    private Counter retryCounter;

    @PostConstruct
    void init() {
        this.createdCounter = Counter.builder("business_settlement_created_total")
                .description("정산 생성 건수")
                .register(meterRegistry);
        this.amountCounter = Counter.builder("business_settlement_amount_total")
                .description("정산 금액 누적 합계")
                .register(meterRegistry);
        this.retryCounter = Counter.builder("business_settlement_retry_total")
                .description("정산 재처리 요청 건수")
                .register(meterRegistry);
    }

    public void incrementCreated() {
        createdCounter.increment();
    }

    public void addAmount(double amount) {
        amountCounter.increment(amount);
    }

    public void incrementRetry() {
        retryCounter.increment();
    }
}
