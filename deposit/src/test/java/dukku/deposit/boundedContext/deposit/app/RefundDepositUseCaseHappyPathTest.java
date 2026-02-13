package dukku.deposit.boundedContext.deposit.app;

import dukku.common.global.event.DomainEvent;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.deposit.event.DepositRefundedEvent;
import dukku.common.shared.deposit.type.DepositHistoryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundDepositUseCaseHappyPathTest {

    @Mock
    private IncreaseDepositUseCase increaseDepositUseCase;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private RefundDepositUseCase useCase;

    @Test
    @DisplayName("환불 액면금액이 있을 때 예치금을 롤백하고 DepositRefundedEvent를 발행한다")
    void refundDepositSuccessShouldIncreaseDepositAndPublishEvent() {
        // given: DONE 상태 예치금이 1,000인 사용자가 있다.
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();
        UUID paymentUuid = UUID.randomUUID();
        UUID refundUuid = UUID.randomUUID();
        Long refundAmount = 2000L;

        // when: 환불 롤백 유스케이스를 실행한다.
        useCase.execute(userUuid, refundAmount, orderUuid, paymentUuid, refundUuid);

        // then: 예치금 롤백이 ROLLBACK 히스토리로 반영되고 이벤트가 deposit.refunded 토픽 payload로 발행된다.
        verify(increaseDepositUseCase).increase(userUuid, refundAmount, DepositHistoryType.ROLLBACK, orderUuid);

        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        assertThat(eventCaptor.getValue()).isInstanceOf(DepositRefundedEvent.class);
        DepositRefundedEvent event = (DepositRefundedEvent) eventCaptor.getValue();
        assertThat(event.refundId()).isEqualTo(refundUuid);
        assertThat(event.paymentUuid()).isEqualTo(paymentUuid);
        assertThat(event.orderUuid()).isEqualTo(orderUuid);
        assertThat(event.userUuid()).isEqualTo(userUuid);
        assertThat(event.amount()).isEqualTo(refundAmount);
    }
}