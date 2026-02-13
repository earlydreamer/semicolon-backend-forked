package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.IssueLogDto;
import dukku.common.shared.coupon.type.IssueResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueLogManager {
    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentLinkedQueue<IssueLogDto> logQueue = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 1000;

    // 중복 플러시 방지를 위한 플래그
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    @Async("logTaskExecutor")
    public void record(UUID couponUuid, UUID userUuid, IssueResult result, LocalDateTime requestedAt) {
        logQueue.add(new IssueLogDto(couponUuid, userUuid, result, requestedAt));

        // 큐가 가득 차면 즉시 플러시 트리거
        if (logQueue.size() >= BATCH_SIZE) {
            flushLogs();
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void scheduledFlush() {
        flushLogs();
    }

    public void flushLogs() {
        // 이미 플러시 중이거나 큐가 비었으면 스킵
        if (logQueue.isEmpty() || !isFlushing.compareAndSet(false, true)) {
            return;
        }

        try {
            while (!logQueue.isEmpty()) {
                List<IssueLogDto> logsToSave = new ArrayList<>(BATCH_SIZE);

                for (int i = 0; i < BATCH_SIZE; i++) {
                    IssueLogDto log = logQueue.poll();
                    if (log == null) break;
                    logsToSave.add(log);
                }

                if (logsToSave.isEmpty()) break;

                batchInsert(logsToSave);
            }
        } finally {
            isFlushing.set(false);
        }
    }

    private void batchInsert(List<IssueLogDto> logsToSave) {
        try {
            String sql = "INSERT INTO coupon_issue_logs (coupon_uuid, user_uuid, result, requested_at, processed_at, elapsed_ms) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            LocalDateTime now = LocalDateTime.now();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    IssueLogDto item = logsToSave.get(i);
                    // item이 null일 가능성은 없으므로 바로 접근
                    long elapsed = Duration.between(item.requestedAt(), now).toMillis();

                    ps.setObject(1, item.couponUuid());
                    ps.setObject(2, item.userUuid());
                    ps.setString(3, item.result().name());
                    ps.setTimestamp(4, Timestamp.valueOf(item.requestedAt()));
                    ps.setTimestamp(5, Timestamp.valueOf(now));
                    ps.setLong(6, elapsed);
                }

                @Override
                public int getBatchSize() {
                    return logsToSave.size();
                }
            });
        } catch (Exception e) {
            log.error("Failed to flush {} logs to DB", logsToSave.size(), e);
        }
    }
}