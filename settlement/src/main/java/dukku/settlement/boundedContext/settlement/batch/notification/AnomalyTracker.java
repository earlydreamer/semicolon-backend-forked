package dukku.settlement.boundedContext.settlement.batch.notification;

import dukku.common.shared.settlement.type.AnomalyType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이상거래 탐지 결과 추적기
 * - 배치 실행 중 탐지된 이상거래를 타입별로 집계
 * - Slack 알림에서 이상거래 통계를 표시하기 위해 사용
 * - 배치 Job 완료 후 clear() 호출하여 데이터 정리
 */
@Component
public class AnomalyTracker {

    private final ConcurrentHashMap<AnomalyType, List<AnomalyRecord>> data = new ConcurrentHashMap<>();

    /**
     * 이상거래 탐지 기록
     */
    public void record(AnomalyType type, UUID settlementUuid, UUID orderId, String description) {
        data.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new AnomalyRecord(settlementUuid, orderId, description));
    }

    /**
     * 탐지된 이상거래가 있는지 확인
     */
    public boolean hasAnomalies() {
        return !data.isEmpty();
    }

    /**
     * CRITICAL 이상거래가 있는지 확인
     */
    public boolean hasCriticalAnomalies() {
        return data.keySet().stream().anyMatch(AnomalyType::isCritical);
    }

    /**
     * 전체 탐지 결과 조회 (타입별)
     */
    public Map<AnomalyType, List<AnomalyRecord>> getAll() {
        Map<AnomalyType, List<AnomalyRecord>> result = new LinkedHashMap<>();
        for (var entry : data.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return result;
    }

    /**
     * 데이터 정리 (배치 완료 후 호출)
     */
    public void clear() {
        data.clear();
    }

    public record AnomalyRecord(UUID settlementUuid, UUID orderId, String description) {
    }
}
