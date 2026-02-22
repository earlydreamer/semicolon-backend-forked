package dukku.settlement.boundedContext.settlement.batch.notification;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Skip 사유 추적기
 * - Skip 발생 시 사유를 Job/Step 단위로 집계
 * - Slack 알림에서 Skip 사유별 통계를 표시하기 위해 사용
 */
@Component
public class SkipReasonTracker {

    // jobExecutionId → stepName → SkipReasonType → count
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, ConcurrentHashMap<SkipReasonType, AtomicInteger>>> data
            = new ConcurrentHashMap<>();

    /**
     * Skip 사유 기록
     */
    public void record(long jobExecutionId, String stepName, SkipReasonType reason) {
        data.computeIfAbsent(jobExecutionId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(stepName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(reason, k -> new AtomicInteger())
                .incrementAndGet();
    }

    /**
     * 특정 Step의 Skip 사유별 통계 조회
     */
    public Map<SkipReasonType, Integer> getStepSkipReasons(long jobExecutionId, String stepName) {
        var jobData = data.get(jobExecutionId);
        if (jobData == null) return Map.of();

        var stepData = jobData.get(stepName);
        if (stepData == null) return Map.of();

        Map<SkipReasonType, Integer> result = new LinkedHashMap<>();
        for (var entry : stepData.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    /**
     * Job 완료 후 데이터 정리
     */
    public void clear(long jobExecutionId) {
        data.remove(jobExecutionId);
    }
}
