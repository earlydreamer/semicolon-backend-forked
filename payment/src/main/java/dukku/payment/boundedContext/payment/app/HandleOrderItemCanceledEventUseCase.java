package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.order.event.OrderItemCanceledEvent;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.exception.PaymentNotFoundException;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 상품 취소 이벤트 처리 UseCase
 * - Order 모듈에서 상품이 취소되었을 때 발행되는 이벤트를 수신
 * - 해당 상품에 대한 결제 정보를 찾아서 환불 처리 트리거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleOrderItemCanceledEventUseCase {

        private final PaymentSupport support;
        private final RefundPaymentUseCase refundPaymentUseCase;

        @Transactional
        public void execute(OrderItemCanceledEvent event) {
                log.info("주문 상품 취소 이벤트 처리 시작 - orderItemUuid: {}", event.orderItemUuid());

                // 1. 해당 주문 상품(OrderItemUuid)이 포함된 결제 내역 조회
                Payment payment = support.findPaymentByOrderItemUuid(event.orderItemUuid())
                                .orElseThrow(() -> {
                                        log.error("해당 주문 상품에 대한 결제 내역을 찾을 수 없습니다. orderItemUuid: {}",
                                                        event.orderItemUuid());
                                        return new PaymentNotFoundException();
                                });

                // 2. 환불 가능 상태 확인 (이미 취소된 경우 중복 처리 방지)
                if (payment.getPaymentStatus() == PaymentStatus.CANCELED) {
                        log.warn("이미 전체 취소된 결제입니다. skip 처리. paymentUuid: {}", payment.getUuid());
                        return;
                }

                // 3. 결제 항목 중 해당 OrderItem 찾기
                PaymentOrderItem targetItem = payment.getItems().stream()
                                .filter(item -> item.getOrderItemUuid().equals(event.orderItemUuid()))
                                .findFirst()
                                .orElseThrow(() -> new PaymentNotFoundException("결제 내역 내에 해당 상품이 존재하지 않습니다."));

                // 4. 환불 요청 생성 (단일 상품 취소이므로 해당 상품 금액만큼)
                PaymentRefundRequest request = PaymentRefundRequest.builder()
                                .paymentUuid(payment.getUuid())
                                .orderUuid(payment.getOrderUuid())
                                .refundAmount(targetItem.getPrice())
                                .reason("주문 전 상품 취소 (Item: " + event.orderItemUuid() + ")")
                                .items(List.of(new PaymentRefundRequest.RefundItemInfo(
                                                targetItem.getOrderItemUuid(),
                                                targetItem.getPrice())))
                                .build();

                // 5. 환불 실행 (멱등성 키로 orderItemUuid 사용)
                refundPaymentUseCase.execute(request, "CANCEL_" + event.orderItemUuid());

                log.info("주문 상품 취소에 따른 환불 트리거 완료 - orderItemUuid: {}, paymentUuid: {}",
                                event.orderItemUuid(), payment.getUuid());
        }
}
