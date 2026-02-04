package dukku.semicolon.boundedContext.settlement.out;

import dukku.semicolon.shared.settlement.dto.BatchJobStatisticsResponse.*;
import dukku.semicolon.shared.settlement.dto.BatchStepStatisticsResponse.StepPerformance;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 배치 메타테이블 조회 Repository
 * Spring Batch 메타테이블(BATCH_JOB_EXECUTION, BATCH_JOB_INSTANCE, BATCH_STEP_EXECUTION)을 조회
 */
@Repository
@RequiredArgsConstructor
public class BatchMetaRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 기간 내 배치 실행 현황 조회 (Job별, 상태별)
     */
    public List<JobStatusCount> findJobStatusByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT jin.JOB_NAME, jex.STATUS, COUNT(*) AS count
            FROM BATCH_JOB_EXECUTION jex
            JOIN BATCH_JOB_INSTANCE jin ON jex.JOB_INSTANCE_ID = jin.JOB_INSTANCE_ID
            WHERE DATE(jex.START_TIME) BETWEEN ? AND ?
            GROUP BY jin.JOB_NAME, jex.STATUS
            ORDER BY jin.JOB_NAME, jex.STATUS
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new JobStatusCount(
                rs.getString("JOB_NAME"),
                rs.getString("STATUS"),
                rs.getLong("count")
        ), startDate, endDate);
    }

    /**
     * 기간 내 실패한 배치 조회
     */
    public List<FailedJobInfo> findFailedJobs(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT jex.JOB_EXECUTION_ID, jin.JOB_NAME, jex.START_TIME, jex.EXIT_MESSAGE
            FROM BATCH_JOB_EXECUTION jex
            JOIN BATCH_JOB_INSTANCE jin ON jex.JOB_INSTANCE_ID = jin.JOB_INSTANCE_ID
            WHERE jex.STATUS = 'FAILED'
            AND DATE(jex.START_TIME) BETWEEN ? AND ?
            ORDER BY jex.START_TIME DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new FailedJobInfo(
                rs.getLong("JOB_EXECUTION_ID"),
                rs.getString("JOB_NAME"),
                rs.getTimestamp("START_TIME") != null
                        ? rs.getTimestamp("START_TIME").toLocalDateTime()
                        : null,
                rs.getString("EXIT_MESSAGE")
        ), startDate, endDate);
    }

    /**
     * 재시작 가능한 Job 조회 (FAILED 상태이면서 동일 인스턴스에 COMPLETED가 없는 것)
     */
    public List<RestartableJobInfo> findRestartableJobs(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT jex.JOB_EXECUTION_ID, jin.JOB_NAME, jex.START_TIME, jex.EXIT_MESSAGE
            FROM BATCH_JOB_EXECUTION jex
            JOIN BATCH_JOB_INSTANCE jin ON jex.JOB_INSTANCE_ID = jin.JOB_INSTANCE_ID
            WHERE jex.STATUS = 'FAILED'
            AND DATE(jex.START_TIME) BETWEEN ? AND ?
            AND NOT EXISTS (
                SELECT 1 FROM BATCH_JOB_EXECUTION jex2
                WHERE jex2.JOB_INSTANCE_ID = jin.JOB_INSTANCE_ID
                AND jex2.STATUS = 'COMPLETED'
            )
            ORDER BY jex.START_TIME DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RestartableJobInfo(
                rs.getLong("JOB_EXECUTION_ID"),
                rs.getString("JOB_NAME"),
                rs.getTimestamp("START_TIME") != null
                        ? rs.getTimestamp("START_TIME").toLocalDateTime()
                        : null,
                rs.getString("EXIT_MESSAGE")
        ), startDate, endDate);
    }

    /**
     * 기간 내 가장 많이 발생하는 에러 TOP 10
     */
    public List<ErrorOccurrence> findTopErrors(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT SUBSTRING(EXIT_MESSAGE, 1, 200) AS error_msg, COUNT(*) AS occurrence
            FROM BATCH_JOB_EXECUTION
            WHERE STATUS = 'FAILED'
            AND DATE(START_TIME) BETWEEN ? AND ?
            AND EXIT_MESSAGE IS NOT NULL
            AND EXIT_MESSAGE != ''
            GROUP BY SUBSTRING(EXIT_MESSAGE, 1, 200)
            ORDER BY occurrence DESC
            LIMIT 10
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ErrorOccurrence(
                rs.getString("error_msg"),
                rs.getLong("occurrence")
        ), startDate, endDate);
    }

    /**
     * 기간 내 정산 처리 건수 (일별)
     */
    public List<DailySettlementCount> findSettlementCounts(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT DATE(jex.START_TIME) AS date, SUM(sex.WRITE_COUNT) AS total_settlements
            FROM BATCH_STEP_EXECUTION sex
            JOIN BATCH_JOB_EXECUTION jex ON sex.JOB_EXECUTION_ID = jex.JOB_EXECUTION_ID
            JOIN BATCH_JOB_INSTANCE jin ON jex.JOB_INSTANCE_ID = jin.JOB_INSTANCE_ID
            WHERE jin.JOB_NAME = 'settlementJob'
            AND jex.STATUS = 'COMPLETED'
            AND DATE(jex.START_TIME) BETWEEN ? AND ?
            GROUP BY DATE(jex.START_TIME)
            ORDER BY date DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DailySettlementCount(
                rs.getString("date"),
                rs.getLong("total_settlements")
        ), startDate, endDate);
    }

    /**
     * 기간 내 Step별 평균 처리 시간 및 성능 통계
     */
    public List<StepPerformance> findStepPerformances(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                STEP_NAME,
                AVG(TIMESTAMPDIFF(SECOND, START_TIME, END_TIME)) AS avg_sec,
                AVG(READ_COUNT) AS avg_read,
                AVG(WRITE_COUNT) AS avg_write,
                AVG(READ_SKIP_COUNT + PROCESS_SKIP_COUNT + WRITE_SKIP_COUNT) AS avg_skip,
                COUNT(*) AS total_executions
            FROM BATCH_STEP_EXECUTION
            WHERE STATUS = 'COMPLETED'
            AND DATE(START_TIME) BETWEEN ? AND ?
            AND START_TIME IS NOT NULL
            AND END_TIME IS NOT NULL
            GROUP BY STEP_NAME
            ORDER BY STEP_NAME
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StepPerformance(
                rs.getString("STEP_NAME"),
                rs.getDouble("avg_sec"),
                rs.getDouble("avg_read"),
                rs.getDouble("avg_write"),
                rs.getDouble("avg_skip"),
                rs.getLong("total_executions")
        ), startDate, endDate);
    }
}
