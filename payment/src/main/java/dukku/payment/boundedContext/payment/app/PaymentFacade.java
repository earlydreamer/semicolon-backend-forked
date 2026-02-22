package dukku.payment.boundedContext.payment.app;

import dukku.common.shared.deposit.event.DepositRefundFailedEvent;
import dukku.common.shared.payment.dto.PaymentConfirmRequest;
import dukku.common.shared.payment.dto.PaymentConfirmResponse;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.dto.PaymentRequest;
import dukku.common.shared.payment.dto.PaymentResponse;
import dukku.common.shared.payment.dto.PaymentResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Payment 도메인 Facade
 *
 * <p>
 * Controller가 호출하는 진입점
 *
 * @see RequestPaymentUseCase 결제 요청
 * @see ConfirmPaymentUseCase 결제 승인
 * @see FindPaymentUseCase 결제 조회
 * @see RefundPaymentUseCase 환불 처리
 * @see CompleteRefundUseCase 환불 완료 처리
 * @see HandleRefundFailureUseCase 환불 실패 처리
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentFacade {

    private final RequestPaymentUseCase requestPayment;
    private final ConfirmPaymentUseCase confirmPayment;
    private final FindPaymentUseCase findPayment;
    private final RefundPaymentUseCase refundPayment;
    private final CompensatePaymentUseCase compensatePayment;
    private final CompleteRefundUseCase completeRefund;
    private final HandleRefundFailureUseCase handleRefundFailure;
    private final HandlePartialRefundEventUseCase handlePartialRefundEvent;
    private final HandleOrderItemCanceledEventUseCase handleOrderItemCanceledEvent;

    /**
     * 결제 요청 (준비)
     *
     * <p>
     * 프론트에서 결제 준비 요청 시 토스 결제창 호출에 필요한 정보 반환
     *
     * @param request        결제 요청 정보
     * @param idempotencyKey 멱등성 키
     * @return 토스 결제창 호출 정보
     */
    public PaymentResponse requestPayment(PaymentRequest request, String idempotencyKey) {
        return requestPayment.execute(request, idempotencyKey);
    }

    /**
     * 결제 승인 확정
     *
     * <p>
     * 토스 인증 완료 후 백엔드에서 최종 승인처리
     *
     * @param request        토스 인증 정보
     * @param idempotencyKey 멱등성 키
     * @return 승인 결과
     */
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request, String idempotencyKey) {
        return confirmPayment.execute(request, idempotencyKey);
    }

    /**
     * 결제 내역 조회
     *
     * @param paymentUuid 결제 UUID
     * @return 결제 상세 정보
     */
    @Transactional(readOnly = true)
    public PaymentResultResponse findPaymentResult(UUID paymentUuid) {
        return findPayment.execute(paymentUuid).toPaymentResultResponse();
    }

    /**
     * 환불 요청
     *
     * @param request        환불 요청 정보
     * @param idempotencyKey 멱등성 키
     * @return 환불 결과
     */
    public PaymentRefundResponse refundPayment(PaymentRefundRequest request, String idempotencyKey) {
        return refundPayment.execute(request, idempotencyKey);
    }

    /**
     * 보상 트랜잭션 (결제 취소)
     *
     * @param orderUuid 주문 UUID
     * @param reason    취소 사유
     */
    public void compensatePayment(UUID orderUuid, String reason) {
        compensatePayment.execute(orderUuid, reason);
    }

    /**
     * 환불 완료 처리
     *
     * @param refundUuid  환불 UUID
     * @param paymentUuid 결제 UUID
     */
    public void completeRefund(UUID refundUuid, UUID paymentUuid) {
        completeRefund.execute(refundUuid, paymentUuid);
    }

    /**
     * 환불 실패 처리
     *
     * @param event 예치금 환불 실패 이벤트
     */
    public void handleRefundFailure(DepositRefundFailedEvent event) {
        handleRefundFailure.execute(event.refundUuid(), event.paymentUuid(), event.reason());
    }

    /**
     * 부분 환불 이벤트 처리 (SAGA)
     *
     * @param event 부분 환불 완료 이벤트
     */
    public void handlePartialRefund(dukku.common.shared.order.event.PartialRefundRequestedEvent event) {
        handlePartialRefundEvent.execute(event);
    }

    /**
     * 주문 상품 취소 이벤트 처리 (SAGA)
     *
     * @param event 주문 상품 취소 이벤트
     */
    public void handleOrderItemCanceled(dukku.common.shared.order.event.OrderItemCanceledEvent event) {
        handleOrderItemCanceledEvent.execute(event);
    }
}
