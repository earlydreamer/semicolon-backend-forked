package dukku.semicolon.boundedContext.deposit.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.semicolon.boundedContext.deposit.entity.enums.DepositHistoryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 환불 처리 UseCase (Saga 참여)
 *
 * <p>
 * 환불 발생 시 예치금을 롤백(재적립)하고 결과를 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundDepositUseCase {

    private final IncreaseDepositUseCase increaseDepositUseCase;
    private final EventPublisher eventPublisher;

    /**
     * 환불 처리 실행
     *
     * @param userUuid  사용자 UUID
     * @param amount    환불 금액
     * @param orderUuid 주문 UUID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userUuid, Long amount, UUID orderUuid) {
        if (amount == null || amount <= 0) {
            return;
        }

        try {
            increaseDepositUseCase.increase(userUuid, amount, DepositHistoryType.ROLLBACK, orderUuid);

            eventPublisher.publish(new DepositRefundedEvent(orderUuid, userUuid, amount));

        } catch (Exception e) {
            log.error("[예치금 환불/롤백 실패] userUuid={}, amount={}, orderUuid={}", userUuid, amount, orderUuid, e);
        }
    }
}
