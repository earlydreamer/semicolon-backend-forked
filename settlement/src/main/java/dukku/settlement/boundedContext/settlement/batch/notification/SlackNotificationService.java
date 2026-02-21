package dukku.settlement.boundedContext.settlement.batch.notification;

import dukku.common.shared.settlement.type.AnomalyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slack 알림 서비스
 * - 정산 배치 완료 시 Slack으로 알림 전송
 * - 이상거래 탐지 결과 포함
 */
@Slf4j
@Service
public class SlackNotificationService {

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate;
    private final BatchFailureDiagnoser failureDiagnoser;
    private final SkipReasonTracker skipReasonTracker;
    private final AnomalyTracker anomalyTracker;

    public SlackNotificationService(@Qualifier("slackRestTemplate") RestTemplate restTemplate,
                                    BatchFailureDiagnoser failureDiagnoser,
                                    SkipReasonTracker skipReasonTracker,
                                    AnomalyTracker anomalyTracker) {
        this.restTemplate = restTemplate;
        this.failureDiagnoser = failureDiagnoser;
        this.skipReasonTracker = skipReasonTracker;
        this.anomalyTracker = anomalyTracker;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 정산 배치 완료 알림 전송
     */
    public void sendJobCompletionNotification(JobExecution jobExecution) {
        String message = buildMessage(jobExecution);
        sendSlackMessage(message);
    }

    private String buildMessage(JobExecution jobExecution) {
        StringBuilder sb = new StringBuilder();

        String status = jobExecution.getStatus().toString();
        String statusEmoji = "COMPLETED".equals(status) ? ":white_check_mark:" : ":x:";

        // 헤더
        sb.append(statusEmoji).append(" *정산 배치 완료 알림*\n\n");

        // Job 정보
        sb.append("*Job Name:* `").append(jobExecution.getJobInstance().getJobName()).append("`\n");
        sb.append("*Job Parameters:* `").append(jobExecution.getJobParameters()).append("`\n");
        sb.append("*Status:* ").append(status).append("\n");
        sb.append("*Start Time:* ").append(formatTime(jobExecution.getStartTime())).append("\n");
        sb.append("*End Time:* ").append(formatTime(jobExecution.getEndTime())).append("\n\n");

        // 실패 원인 진단
        if (!"COMPLETED".equals(status)) {
            String diagnosis = failureDiagnoser.diagnose(jobExecution);
            if (diagnosis != null) {
                sb.append(diagnosis).append("\n");
            }
        }

        // 이상거래 탐지 결과
        buildAnomalySection(sb);

        // Step별 통계
        sb.append("*Step별 통계:*\n");
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            sb.append("━━━━━━━━━━━━━━━━━━━━\n");
            long readCount = stepExecution.getReadCount();
            long processCount = readCount - stepExecution.getFilterCount();
            long writeCount = stepExecution.getWriteCount();

            sb.append(":arrow_forward: *Step:* `").append(stepExecution.getStepName()).append("`\n");
            sb.append("  • Read: ").append(readCount)
                    .append(" → Process: ").append(processCount)
                    .append(" → Write: ").append(writeCount).append("\n");
            sb.append("  • Skip: ").append(stepExecution.getSkipCount())
                    .append(" / Rollback: ").append(stepExecution.getRollbackCount()).append("\n");

            // Skip 사유별 통계
            if (stepExecution.getSkipCount() > 0) {
                Map<SkipReasonType, Integer> skipReasons = skipReasonTracker.getStepSkipReasons(
                        jobExecution.getId(), stepExecution.getStepName());
                if (!skipReasons.isEmpty()) {
                    sb.append("  • *Skip 사유:*\n");
                    for (Map.Entry<SkipReasonType, Integer> entry : skipReasons.entrySet()) {
                        sb.append("    - ").append(entry.getKey().getDescription())
                                .append(": ").append(entry.getValue()).append("건\n");
                    }
                }
            }
        }

        // 추적 데이터 정리
        skipReasonTracker.clear(jobExecution.getId());
        anomalyTracker.clear();

        return sb.toString();
    }

    private void buildAnomalySection(StringBuilder sb) {
        if (!anomalyTracker.hasAnomalies()) {
            return;
        }

        Map<AnomalyType, List<AnomalyTracker.AnomalyRecord>> anomalies = anomalyTracker.getAll();

        // CRITICAL 이상거래
        boolean hasCritical = anomalyTracker.hasCriticalAnomalies();
        if (hasCritical) {
            sb.append(":rotating_light: *CRITICAL 이상거래 탐지*\n");
            for (var entry : anomalies.entrySet()) {
                AnomalyType type = entry.getKey();
                if (!type.isCritical()) continue;
                List<AnomalyTracker.AnomalyRecord> records = entry.getValue();

                sb.append("  • *").append(type.name()).append("* (")
                        .append(type.getDescription()).append(") — ").append(records.size()).append("건\n");
                for (AnomalyTracker.AnomalyRecord record : records) {
                    sb.append("    - `").append(record.settlementUuid()).append("` ")
                            .append(record.description()).append("\n");
                }
            }
            sb.append("\n");
        }

        // HIGH 이상거래
        boolean hasHigh = anomalies.keySet().stream().anyMatch(t -> !t.isCritical());
        if (hasHigh) {
            sb.append(":warning: *HIGH 이상거래 탐지*\n");
            for (var entry : anomalies.entrySet()) {
                AnomalyType type = entry.getKey();
                if (type.isCritical()) continue;
                List<AnomalyTracker.AnomalyRecord> records = entry.getValue();

                sb.append("  • *").append(type.name()).append("* (")
                        .append(type.getDescription()).append(") — ").append(records.size()).append("건\n");
                for (AnomalyTracker.AnomalyRecord record : records) {
                    sb.append("    - `").append(record.settlementUuid()).append("` ")
                            .append(record.description()).append("\n");
                }
            }
            sb.append("\n");
        }
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(FORMATTER) : "-";
    }

    private void sendSlackMessage(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[Slack 알림 스킵] webhook URL이 설정되지 않았습니다.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> payload = new HashMap<>();
            payload.put("text", message);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);

            log.info("[Slack 알림 전송 완료]");
        } catch (Exception e) {
            log.error("[Slack 알림 전송 실패] error={}", e.getMessage(), e);
        }
    }
}
