package dukku.semicolon.boundedContext.settlement.app;

import dukku.semicolon.boundedContext.settlement.out.BatchMetaRepository;
import dukku.semicolon.shared.settlement.dto.BatchJobStatisticsResponse;
import dukku.semicolon.shared.settlement.dto.BatchJobStatisticsResponse.*;
import dukku.semicolon.shared.settlement.dto.BatchStepStatisticsResponse;
import dukku.semicolon.shared.settlement.dto.BatchStepStatisticsResponse.StepPerformance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 배치 통계 조회 UseCase
 * Spring Batch 메타테이블 기반 통계
 */
@Component
@RequiredArgsConstructor
public class GetBatchStatisticsUseCase {

    private final BatchMetaRepository batchMetaRepository;

    /**
     * 배치 Job 통계 조회
     */
    @Transactional(readOnly = true)
    public BatchJobStatisticsResponse getJobStatistics(LocalDate startDate, LocalDate endDate) {
        List<JobStatusCount> jobStatus = batchMetaRepository.findJobStatusByDateRange(startDate, endDate);
        List<FailedJobInfo> failedJobs = batchMetaRepository.findFailedJobs(startDate, endDate);
        List<RestartableJobInfo> restartableJobs = batchMetaRepository.findRestartableJobs(startDate, endDate);
        List<ErrorOccurrence> topErrors = batchMetaRepository.findTopErrors(startDate, endDate);
        List<DailySettlementCount> settlementCounts = batchMetaRepository.findSettlementCounts(startDate, endDate);

        return new BatchJobStatisticsResponse(
                jobStatus,
                failedJobs,
                restartableJobs,
                topErrors,
                settlementCounts
        );
    }

    /**
     * 배치 Step 통계 조회
     */
    @Transactional(readOnly = true)
    public BatchStepStatisticsResponse getStepStatistics(LocalDate startDate, LocalDate endDate) {
        List<StepPerformance> stepPerformances = batchMetaRepository.findStepPerformances(startDate, endDate);
        return new BatchStepStatisticsResponse(stepPerformances);
    }
}
