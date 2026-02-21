package dukku.settlement.boundedContext.settlement.batch.listener;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.settlement.boundedContext.settlement.batch.notification.SkipReasonTracker;
import dukku.settlement.boundedContext.settlement.batch.notification.SkipReasonType;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * 정산 대상 생성 Step Skip 리스너 (Step 1에서 사용)
 * - Skip 발생 시 사유를 분류하여 SkipReasonTracker에 기록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSettlementSkipListener implements SkipListener<ConfirmedOrderItemResponse, Settlement> {

    private final SkipReasonTracker skipReasonTracker;

    @Override
    public void onSkipInRead(Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-READ] Step 1 읽기 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
        log.error("  - Exception: {}", t.getClass().getSimpleName());
        log.error("  - Message: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(ConfirmedOrderItemResponse item, Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-PROCESS] Step 1 처리 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
        log.error("  - OrderItem UUID: {}", item != null ? item.orderItemUuid() : "null");
        log.error("  - Exception: {}", t.getClass().getSimpleName());
        log.error("  - Message: {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(Settlement item, Throwable t) {
        SkipReasonType reason = SkipReasonType.classify(t);
        recordSkipReason(reason);

        log.error("[SKIP-WRITE] Step 1 저장 중 에러 발생 - Skip 처리됨. 사유={}", reason.getDescription());
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
