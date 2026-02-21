package dukku.settlement.boundedContext.settlement.batch.processor;

import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.settlement.boundedContext.settlement.entity.SettlementSchedulePolicy;
import dukku.settlement.boundedContext.settlement.batch.config.SettlementBatchProperties;
import dukku.common.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.common.shared.payment.dto.PaymentInternalResponse;
import dukku.common.shared.payment.out.PaymentApiClient;
import dukku.common.shared.settlement.exception.SettlementProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

/**
 * Step 1: 정산 대상 생성 Processor
 * - 구매 확정된 OrderItem → Settlement 변환
 * - PENDING 상태로 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSettlementProcessor implements ItemProcessor<ConfirmedOrderItemResponse, Settlement> {

    private final DepositApiClient depositApiClient;
    private final PaymentApiClient paymentApiClient;
    private final SettlementBatchProperties batchProperties;

    @Override
    public Settlement process(ConfirmedOrderItemResponse orderItem) throws Exception {
        log.debug("[Step 1 Processor] Settlement 생성 시작. orderItemUuid={}", orderItem.orderItemUuid());

        try {
            // 1. 판매자의 예치금 계좌 UUID 조회
            UUID depositUuid = depositApiClient.getDepositUuid(orderItem.sellerUuid());

            if (depositUuid == null) {
                log.error("[Step 1 Processor] 판매자 예치금 계좌 없음. sellerUuid={}", orderItem.sellerUuid());
                throw SettlementProcessingException.depositAccountNotFound(String.valueOf(orderItem.sellerUuid()));
            }

            // 2. Payment 정보 조회
            PaymentInternalResponse payment = paymentApiClient.getPaymentByOrderUuid(orderItem.orderUuid());
            UUID paymentUuid = payment.getPaymentUuid();

            if (paymentUuid == null) {
                log.error("[Step 1 Processor] 결제 정보 없음. orderUuid={}", orderItem.orderUuid());
                throw new SettlementProcessingException("결제 정보를 찾을 수 없습니다. orderUuid=" + orderItem.orderUuid());
            }

            // 3. Settlement 생성 (PENDING 상태)
            Settlement settlement = Settlement.create(
                    orderItem.sellerUuid(),
                    orderItem.buyerUuid(),
                    paymentUuid,
                    orderItem.orderUuid(),
                    orderItem.orderItemUuid(),
                    depositUuid,
                    (long) orderItem.productPrice(),
                    batchProperties.getFeeRate(),
                    SettlementSchedulePolicy.nextReservationDate()
            );

            log.info("[Step 1 Processor] Settlement 생성 완료. settlementUuid={}, orderItemUuid={}, amount={}",
                    settlement.getUuid(), orderItem.orderItemUuid(), orderItem.productPrice());

            return settlement;

        } catch (SettlementProcessingException e) {
            // 비즈니스 오류 → Skip 처리
            log.error("[Step 1 Processor-Skip] Settlement 생성 실패. orderItemUuid={}, error={}",
                    orderItem.orderItemUuid(), e.getMessage());
            throw e;

        } catch (Exception e) {
            // 외부 서비스 연결 실패 포함 모든 예상치 못한 오류 → 즉시 Step 실패
            log.error("[Step 1 Processor] 처리 불가 오류. orderItemUuid={}, error={}",
                    orderItem.orderItemUuid(), e.getMessage(), e);
            throw e;
        }
    }
}
