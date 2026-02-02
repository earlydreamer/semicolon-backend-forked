package dukku.semicolon.boundedContext.settlement.in.batch.processor;

import dukku.semicolon.boundedContext.settlement.in.batch.config.SettlementBatchProperties;
import dukku.semicolon.shared.deposit.out.depositApiClient.DepositApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 1: 정산 대상 생성 Processor
 * - 구매 확정된 OrderItem → Settlement 변환
 * - PENDING 상태로 생성
 *
 * [TODO] Order BC API 구현 후 활성화
 * - ConfirmedOrderItemDto를 받아 Settlement 엔티티 생성
 * - 수수료 계산 및 정산 예약일 설정
 *
 * [TODO] PaymentApiClient 구현 필요
 * - 주문에 대한 결제 정보 조회 (paymentUuid)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSettlementProcessor {

    private final DepositApiClient depositApiClient;
    private final SettlementBatchProperties batchProperties;
    // TODO: PaymentApiClient paymentApiClient;

    /*
    TODO: Order BC API 구현 후 활성화

    @Override
    public Settlement process(ConfirmedOrderItemDto orderItem) throws Exception {
        log.debug("[Step 1 Processor] Settlement 생성 시작. orderItemUuid={}", orderItem.getOrderItemUuid());

        try {
            // 1. 판매자의 예치금 계좌 UUID 조회
            UUID depositUuid = depositApiClient.getDepositUuid(orderItem.getSellerUuid());

            if (depositUuid == null) {
                log.error("[Step 1 Processor] 판매자 예치금 계좌 없음. sellerUuid={}", orderItem.getSellerUuid());
                throw SettlementProcessingException.depositAccountNotFound(orderItem.getSellerUuid());
            }

            // 2. TODO: Payment 정보 조회 (PaymentApiClient 구현 필요)
            // PaymentDto payment = paymentApiClient.getPaymentByOrderUuid(orderItem.getOrderUuid());
            // UUID paymentUuid = payment.getUuid();
            UUID paymentUuid = null; // TODO: PaymentApiClient 구현 후 수정

            // 3. Settlement 생성 (PENDING 상태)
            Settlement settlement = Settlement.create(
                    orderItem.getSellerUuid(),          // 판매자 UUID
                    orderItem.getBuyerUuid(),           // 구매자 UUID
                    paymentUuid,                        // 결제 UUID (TODO)
                    orderItem.getOrderUuid(),           // 주문 UUID
                    orderItem.getOrderItemUuid(),       // 주문 상품 UUID
                    depositUuid,                        // 예치금 계좌 UUID
                    orderItem.getProductPrice(),        // 총액 (상품 가격)
                    batchProperties.getFeeRate(),       // 수수료율
                    SettlementSchedulePolicy.nextReservationDate() // 정산 예약일
            );

            log.info("[Step 1 Processor] Settlement 생성 완료. settlementUuid={}, orderItemUuid={}, amount={}",
                    settlement.getUuid(), orderItem.getOrderItemUuid(), orderItem.getProductPrice());

            return settlement;

        } catch (SettlementProcessingException e) {
            log.error("[Step 1 Processor-Skip] Settlement 생성 실패. orderItemUuid={}, error={}",
                    orderItem.getOrderItemUuid(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("[Step 1 Processor-Skip] 예상치 못한 오류. orderItemUuid={}, error={}",
                    orderItem.getOrderItemUuid(), e.getMessage(), e);
            throw new SettlementProcessingException(
                    "Settlement 생성 중 오류 발생: " + e.getMessage());
        }
    }
    */
}
