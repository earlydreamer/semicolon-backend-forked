package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.IssueLogDto;
import dukku.common.shared.coupon.type.IssueResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueLogManager {

    private final JdbcTemplate jdbcTemplate;

    // 1. [OOM 방지] 무제한 큐 대신 최대 용량을 설정 (예: 10만 건)
    // 메모리 계산: 객체당 약 100~200byte * 100,000 ≈ 20MB (안전함)
    private final LinkedBlockingQueue<IssueLogDto> logQueue = new LinkedBlockingQueue<>(100_000);

    private static final int BATCH_SIZE = 1000;

    // 동시 실행 방지 Lock (synchronized보다 가벼움)
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    /**
     * 로그 적재 (Non-Blocking)
     * 큐가 가득 찼을 때 대기하지 않고 즉시 false를 반환하여 메인 비즈니스 로직에 영향을 주지 않음.
     */
    public void record(UUID couponUuid, UUID userUuid, IssueResult result, LocalDateTime requestedAt) {
        IssueLogDto logDto = new IssueLogDto(couponUuid, userUuid, result, requestedAt);

        // offer: 큐에 공간이 있으면 true, 꽉 차면 false 반환 (예외 발생 X)
        boolean isSuccess = logQueue.offer(logDto);

        if (!isSuccess) {
            // [중요] 로그 큐가 가득 차면 로그는 유실됩니다. (비즈니스 로직이 우선)
            // 대신 에러 로그를 남겨 모니터링 알림이 가도록 합니다.
            log.error("[로그 유실] 로그 큐가 가득 찼습니다. 사용자 UUID: {}", userUuid);
        }
    }

    /**
     * 주기적 플러시 (1초 주기)
     */
    @Scheduled(fixedDelay = 10000)
    public void scheduledFlush() {
        flushLogs();
    }

    /**
     * [우아한 종료] 서버가 내려갈 때 큐에 남은 잔여 로그를 모두 저장
     */
    @PreDestroy
    public void onShutdown() {
        log.info("애플리케이션 종료 중... 남은 {}건의 로그를 저장합니다.", logQueue.size());
        flushLogs();
    }

    /**
     * 실제 DB 저장 로직
     */
    public void flushLogs() {
        // 이미 플러시 중이거나 큐가 비었으면 스킵
        if (logQueue.isEmpty() || !isFlushing.compareAndSet(false, true)) {
            return;
        }

        try {
            while (!logQueue.isEmpty()) {
                List<IssueLogDto> batchList = new ArrayList<>(BATCH_SIZE);

                // 2. [성능 최적화] drainTo 사용
                // poll()로 루프 도는 것보다 훨씬 빠름 (락 획득 횟수 감소)
                // 큐에서 최대 BATCH_SIZE만큼 꺼내서 batchList에 담음
                int drainedCount = logQueue.drainTo(batchList, BATCH_SIZE);

                if (drainedCount > 0) {
                    batchInsert(batchList);
                }
            }
        } catch (Exception e) {
            log.error("로그 플러시 중 오류가 발생했습니다.", e);
        } finally {
            isFlushing.set(false);
        }
    }

    private void batchInsert(List<IssueLogDto> logsToSave) {
        String sql = "INSERT INTO coupon_issue_logs (coupon_uuid, user_uuid, result, requested_at, processed_at, elapsed_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            LocalDateTime now = LocalDateTime.now();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    IssueLogDto item = logsToSave.get(i);
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
            // DB 연결 오류 등으로 배치가 실패하면, 해당 로그들은 유실됩니다.
            // 재시도 로직을 넣을 수도 있지만, 로그 시스템 특성상 다음 배치를 위해 포기하는 전략을 사용합니다.
            log.error("배치 로그 저장에 실패했습니다. {}건의 로그가 유실됩니다.", logsToSave.size(), e);
        }
    }
}