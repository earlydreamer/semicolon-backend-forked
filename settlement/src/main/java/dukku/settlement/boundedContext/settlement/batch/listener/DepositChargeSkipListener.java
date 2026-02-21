package dukku.settlement.boundedContext.settlement.batch.listener;

import dukku.settlement.boundedContext.settlement.batch.notification.SkipReasonTracker;
import dukku.settlement.boundedContext.settlement.batch.notification.SkipReasonType;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.settlement.boundedContext.settlement.out.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * Settlement Skip 리스너 (Step 2, 3에서 사용)
 * - Skip 발생 시 사유를 분류하여 SkipReasonTracker에 기록
 * - 실패한 Settlement를 FAILED 상태로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DepositChargeSkipListener implements SkipListener<Settlement, Settlement> {

    private final SettlementRepository settlementRepository;
    private final SkipReasonTracker skipReasonTracker;

    @Override
    public void onSkipInRead(Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-READ] 읽기 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
        log.error("  - Exception: {}", t.getClass().getSimpleName());
        log.error("  - Message: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Settlement item, Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-PROCESS] 처리 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
        log.error("  - Settlement UUID: {}", item != null ? item.getUuid() : "null");
        log.error("  - Seller UUID: {}", item != null ? item.getSellerUuid() : "null");
        log.error("  - Exception: {}", t.getClass().getSimpleName());
        log.error("  - Message: {}", t.getMessage());

        // 실패한 Settlement를 FAILED 상태로 변경
        if (item != null) {
            try {
                item.fail();
                settlementRepository.save(item);
                log.info("Settlement 상태를 FAILED로 변경함 - UUID: {}", item.getUuid());
            } catch (Exception e) {
                log.error("Settlement FAILED 상태 변경 실패 - UUID: {}, Error: {}",
                        item.getUuid(), e.getMessage());
            }
        }
    }

    @Override
    public void onSkipInWrite(Settlement item, Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-WRITE] 저장 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
        log.error("  - Settlement UUID: {}", item != null ? item.getUuid() : "null");
        log.error("  - Exception: {}", t.getClass().getSimpleName());
        log.error("  - Message: {}", t.getMessage());
    }

    private void recordSkipReason(SkipReasonType reason) {
        StepContext stepContext = StepSynchronizationManager.getContext();
        if (stepContext != null) {
            StepExecution stepExecution = stepContext.getStepExecution();
            skipReasonTracker.record(
                    stepExecution.getJobExecution().getId(),
                    stepExecution.getStepName(),
                    reason
            );
        }
    }
}
