package dukku.payment.boundedContext.payment.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import dukku.payment.boundedContext.payment.entity.Refund;
import dukku.payment.boundedContext.payment.entity.RefundItem;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.common.shared.payment.type.PaymentStatus;
import dukku.common.shared.payment.type.RefundStatus;
import dukku.common.shared.payment.exception.InvalidRefundAmountException;
import dukku.common.shared.payment.exception.PaymentNotRefundableException;
import dukku.common.shared.payment.dto.PaymentRefundRequest;
import dukku.common.shared.payment.dto.PaymentRefundResponse;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundFailedEvent;
import dukku.common.shared.payment.event.RefundRequestedEvent;
import dukku.payment.boundedContext.payment.out.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 환불 처리 UseCase
 *
 * <p>
 * 결제 완료된 건에 대해 사용자 요청 또는 시스템 이벤트로 환불 수행
 * 외부 자산(PG) 취소 성공 후 내부 결제 데이터(상태/이력) 업데이트
 * 예치금 복구 완료 후 RefundCompletedEvent로 결제-주문 후속 처리 진행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private static final String MESSAGE_NOT_FOUND_IN_RESPONSE = "응답 본문에 메시지가 없습니다.";

    private final PaymentSupport support;
    private final EventPublisher eventPublisher;
    private final TossPaymentClient tossClient;

    /**
     * 환불 요청 처리
     *
     * @param request        환불 요청 정보
     * @param idempotencyKey 멱등성 키
     * @return 환불 결과
     */
    @Transactional
    public PaymentRefundResponse execute(PaymentRefundRequest request, String idempotencyKey) {
        // 1) 멱등성 키로 기존 환불 이력 조회 후 있으면 즉시 응답 재사용
        Optional<Refund> existingRefund = support.findRefundByIdempotencyKey(idempotencyKey);
        if (existingRefund.isPresent()) {
            Refund refund = existingRefund.get();
            Payment payment = refund.getPayment();
            Long pgRefundAmount = refund.getRefundAmountTotal() - refund.getRefundDepositTotal();
            return refund.toPaymentRefundResponse(pgRefundAmount, payment.getTossOrderId());
        }

        // 2) 결제 조회
        Payment payment = support.findPaymentByUuid(request.getPaymentUuid());

        // 3) 환불 대상 결제와 요청 주문 UUID 일치 여부 확인
        validateOrderMatch(payment, request.getOrderUuid());

        // 4) 환불 가능 상태 검증
        validateRefundable(payment);

        // 5) 환불 가능 금액 검증
        validateRefundAmount(payment, request.getRefundAmount());

        // 6) 상태 변경 전 값 저장 (이력 기록용)
        PaymentStatus originStatus = payment.getPaymentStatus();
        Long originAmountPg = payment.getAmountPg();
        Long originDeposit = payment.getPaymentDeposit();

        // 7) 환불 금액 배분 산정
        RefundAllocation allocation = resolveRefundAllocation(payment, request);

        // 8) PG 취소 수행 (외부 시스템 확정 먼저)
        if (allocation.pgRefundAmount() > 0) {
            Map<String, Object> cancelBody = new HashMap<>();
            cancelBody.put("cancelReason", request.getReason());
            cancelBody.put("cancelAmount", allocation.pgRefundAmount());

            Map<String, Object> response;
            try {
                response = tossClient.cancel(payment.getPgPaymentKey(), cancelBody);
            } catch (RuntimeException e) {
                log.error("[CRITICAL][환불 실패] PG 환불 요청 예외: {}. paymentUuid={}",
                        e.getMessage(), payment.getUuid());
                handleFailure(payment, originStatus, originAmountPg, originDeposit,
                        "PG_CANCEL_EXCEPTION: " + e.getMessage());
                publishRefundFailed(
                        payment,
                        allocation,
                        request,
                        PaymentFailureCode.REFUND_PG_CANCEL_EXCEPTION,
                        true,
                        buildFailureReason(PaymentFailureCode.REFUND_PG_CANCEL_EXCEPTION, e.getMessage())
                );
                return PaymentRefundResponse.builder()
                        .success(false).code("PG_CANCEL_EXCEPTION")
                        .message("PG 환불 요청 예외 발생: " + e.getMessage())
                        .build();
            }

            Integer statusCode = extractStatusCode(response);
            String responseMessage = resolveResponseMessage(response);
            if (statusCode == null || isErrorStatus(statusCode)) {
                log.error("[CRITICAL][환불 실패] PG 환불 응답 오류: status={}, body={}, paymentUuid={}",
                        statusCode, response, payment.getUuid());
                handleFailure(payment, originStatus, originAmountPg, originDeposit,
                        "PG_CANCEL_FAILED: " + responseMessage);
                publishRefundFailed(
                        payment,
                        allocation,
                        request,
                        PaymentFailureCode.REFUND_PG_CANCEL_FAILED,
                        false,
                        buildFailureReason(PaymentFailureCode.REFUND_PG_CANCEL_FAILED, responseMessage)
                );
                return PaymentRefundResponse.builder()
                        .success(false).code("PG_CANCEL_FAILED")
                        .message("PG 환불 처리 실패: " + responseMessage)
                        .build();
            }
        }

        // 9) Refund 엔티티 및 항목 환불 내역 생성
        Refund refund = payment.createRefund(
                request.getRefundAmount(),
                allocation.depositRefundAmount(),
                idempotencyKey);

        for (ItemRefundAllocation itemAllocation : allocation.itemRefundAllocations()) {
            refund.addRefundItem(RefundItem.create(
                    refund,
                    itemAllocation.paymentOrderItem(),
                    itemAllocation.refundAmount(),
                    itemAllocation.depositAmount(),
                    itemAllocation.pgAmount()));
        }

        // 10) 예치금 환불이 없으면 즉시 완료 상태 전환
        if (allocation.depositRefundAmount() == 0L) {
            refund.complete();
        }

        support.saveRefund(refund);

        // 11) 결제 상태 및 금액 반영 후 저장
        payment.partialCancel(
                request.getRefundAmount(),
                allocation.pgRefundAmount(),
                allocation.depositRefundAmount());
        support.savePayment(payment);

        PaymentHistoryType historyType = (payment.getPaymentStatus() == PaymentStatus.CANCELED)
                ? PaymentHistoryType.FULL_REFUND_SUCCESS
                : PaymentHistoryType.PARTIAL_REFUND_SUCCESS;
        support.createHistory(payment, historyType, originStatus, originAmountPg, originDeposit);

        publishRefundRequested(payment, refund);
        publishRefundCompleted(payment, refund);

        // 12) 환불 완료 이벤트 조건부 발행 후 응답 반환
        return refund.toPaymentRefundResponse(allocation.pgRefundAmount(), payment.getTossOrderId());
    }

    /**
     * 환불 대상 결제와 주문 UUID 일치 여부 확인
     */
    private void validateOrderMatch(Payment payment, UUID orderUuid) {
        if (orderUuid == null || !payment.getOrderUuid().equals(orderUuid)) {
            throw new InvalidRefundAmountException("환불 요청 주문 UUID가 결제 주문과 일치하지 않습니다.");
        }
    }

    /**
     * 환불 가능한 결제 상태 확인
     */
    private void validateRefundable(Payment payment) {
        PaymentStatus status = payment.getPaymentStatus();
        if (status != PaymentStatus.DONE && status != PaymentStatus.PARTIAL_CANCELED) {
            throw new PaymentNotRefundableException();
        }
    }

    /**
     * 환불 가능 금액 범위 확인
     */
    private void validateRefundAmount(Payment payment, Long refundAmount) {
        if (refundAmount == null || refundAmount <= 0L) {
            throw new InvalidRefundAmountException("환불 금액은 1원 이상이어야 합니다.");
        }

        Long refundableAmount = payment.getAmount() - payment.getRefundTotal();
        if (refundAmount > refundableAmount) {
            throw new InvalidRefundAmountException("요청 금액이 환불 가능 금액을 초과했습니다: " + refundableAmount);
        }
    }

    /**
     * 환불 할당 정책 계산
     *
     * <p>
     * 부분 환불 요청 시 상품 기준 PG/예치금 분배 적용
     */
    private RefundAllocation resolveRefundAllocation(Payment payment, PaymentRefundRequest request) {
        // 전체 금액 환불이면 도메인 기본 분배 로직 사용
        if (request.getItems() == null || request.getItems().isEmpty()) {
            Payment.RefundAllocation allocation = payment.calculateRefundAllocation(request.getRefundAmount());
            return new RefundAllocation(allocation.pgRefundAmount(), allocation.depositRefundAmount(), List.of());
        }

        // 항목별 환불 요청이 있으면 항목별 금액 분배와 중복/기존환불 검증 수행
        List<ItemRefundAllocation> itemRefundAllocations = resolveItemAllocations(payment, request.getItems());
        Long totalItemRefundAmount = itemRefundAllocations.stream()
                .mapToLong(ItemRefundAllocation::refundAmount)
                .sum();

        if (!totalItemRefundAmount.equals(request.getRefundAmount())) {
            throw new InvalidRefundAmountException("요청 항목 금액 합계가 총 환불 금액과 일치하지 않습니다.");
        }

        long totalPgRefund = itemRefundAllocations.stream()
                .mapToLong(ItemRefundAllocation::pgAmount)
                .sum();
        long totalDepositRefund = itemRefundAllocations.stream()
                .mapToLong(ItemRefundAllocation::depositAmount)
                .sum();

        return new RefundAllocation(totalPgRefund, totalDepositRefund, itemRefundAllocations);
    }

    /**
     * 상품별 환불 항목 PG/예치금 분배 계산
     */
    private List<ItemRefundAllocation> resolveItemAllocations(Payment payment,
                                                              List<PaymentRefundRequest.RefundItemInfo> requestItems) {
        // 중복 항목 등록, 존재 여부, 환불 가능 금액 초과 등을 검증하며 항목별 환불 배분
        Set<UUID> duplicatedCheck = new HashSet<>();
        List<ItemRefundAllocation> allocations = new ArrayList<>();
        for (PaymentRefundRequest.RefundItemInfo itemInfo : requestItems) {
            if (itemInfo == null || itemInfo.getOrderItemUuid() == null || itemInfo.getRefundAmount() == null) {
                throw new InvalidRefundAmountException("환불 항목의 주문 상품 UUID/금액이 비어 있습니다.");
            }

            UUID orderItemUuid = itemInfo.getOrderItemUuid();
            if (!duplicatedCheck.add(orderItemUuid)) {
                throw new InvalidRefundAmountException("동일한 주문 상품이 중복 등록되었습니다.");
            }

            PaymentOrderItem paymentOrderItem = support.findPaymentOrderItem(payment.getId(), orderItemUuid)
                    .orElseThrow(() -> new InvalidRefundAmountException("환불 요청한 주문 상품을 찾을 수 없습니다."));

            Long requestedAmount = itemInfo.getRefundAmount();
            if (requestedAmount <= 0L) {
                throw new InvalidRefundAmountException("환불 금액은 1원 이상이어야 합니다.");
            }

            Long alreadyRefundedAmount = support.getRefundedAmountByPaymentOrderItem(paymentOrderItem.getId());
            long couponAmount = paymentOrderItem.getPaymentCoupon() == null ? 0L : paymentOrderItem.getPaymentCoupon();
            long netPaidAmount = paymentOrderItem.getPrice() - couponAmount;
            if (netPaidAmount < 0L) {
                throw new InvalidRefundAmountException("상품 결제 금액 계산이 잘못되었습니다.");
            }

            Long refundableAmount = netPaidAmount - alreadyRefundedAmount;
            if (refundableAmount < 0L) {
                throw new InvalidRefundAmountException("요청 환불 금액이 상품 환불 가능 금액을 초과합니다.");
            }
            if (requestedAmount > refundableAmount) {
                throw new InvalidRefundAmountException("요청 환불 금액이 상품 환불 가능 금액을 초과합니다.");
            }

            Long alreadyRefundedDeposit = support.getRefundedDepositAmountByPaymentOrderItem(paymentOrderItem.getId());
            Long remainingDeposit = paymentOrderItem.getPaymentDeposit() - alreadyRefundedDeposit;
            if (remainingDeposit < 0L) {
                throw new InvalidRefundAmountException("상품 환불된 예치금이 잘못되어 계산할 수 없습니다.");
            }

            Long depositAmount = Math.min(remainingDeposit, requestedAmount);
            Long pgAmount = requestedAmount - depositAmount;

            allocations.add(new ItemRefundAllocation(paymentOrderItem, requestedAmount, depositAmount, pgAmount));
        }

        return allocations;
    }

    /**
     * PG 응답 상태코드 추출
     */
    private Integer extractStatusCode(Map<String, Object> response) {
        // PG 응답에서 statusCode 추출
        if (response == null) {
            return null;
        }

        Object statusCode = response.get("statusCode");
        if (statusCode == null) {
            return null;
        }

        if (statusCode instanceof Number number) {
            return number.intValue();
        }

        if (statusCode instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * 상태코드 에러 여부 판별
     */
    private boolean isErrorStatus(int statusCode) {
        // HTTP 상태코드 규칙으로 에러 여부 판정
        try {
            return HttpStatus.valueOf(statusCode).isError();
        } catch (IllegalArgumentException e) {
            return statusCode >= 400;
        }
    }

    /**
     * PG 응답 메시지 필드 추출
     */
    private String resolveResponseMessage(Map<String, Object> response) {
        // PG 응답 메시지 필드(message/errorMessage) 우선 조회
        if (response == null) {
            return MESSAGE_NOT_FOUND_IN_RESPONSE;
        }

        Object message = response.get("message");
        if (message == null) {
            message = response.get("errorMessage");
        }

        if (message == null) {
            return MESSAGE_NOT_FOUND_IN_RESPONSE;
        }

        return String.valueOf(message);
    }

    private void publishRefundRequested(Payment payment, Refund refund) {
        // PENDING + 예치금 환불이 필요한 건만 사가 시작 이벤트 발행
        if (refund.getRefundStatus() != RefundStatus.PENDING || refund.getRefundDepositTotal() <= 0L) {
            return;
        }

        eventPublisher.publish(new RefundRequestedEvent(
                refund.getUuid(),
                payment.getUuid(),
                payment.getOrderUuid(),
                refund.getRefundAmountTotal(),
                refund.getRefundDepositTotal(),
                payment.getUserUuid(),
                refund.getCreatedAt()));
    }

    private void publishRefundCompleted(Payment payment, Refund refund) {
        // COMPLETED 상태면 결제-주문 연동 후속 처리를 위해 이벤트 발행
        if (refund.getRefundStatus() != RefundStatus.COMPLETED) {
            return;
        }

        eventPublisher.publish(new RefundCompletedEvent(
                refund.getUuid(),
                payment.getUuid(),
                payment.getOrderUuid(),
                refund.getRefundAmountTotal(),
                refund.getRefundDepositTotal(),
                payment.getUserUuid(),
                refund.getCreatedAt()));
    }

    /**
     * PG 취소 실패 시 결제 상태 롤백 실패로 전환, 이력 기록
     */
    private void handleFailure(Payment payment, PaymentStatus originStatus, Long originAmountPg, Long originDeposit,
                               String reason) {
        // PG 실패 시 결제 상태를 롤백 실패로 전환하고 이력 기록
        payment.rollbackFailedStatus();
        support.savePayment(payment);
        support.createHistory(payment, PaymentHistoryType.PAYMENT_ROLLBACK_FAILED, originStatus, originAmountPg,
                originDeposit);
    }

    /**
     * 환불 실패 이벤트 발행
     */
    private void publishRefundFailed(Payment payment, RefundAllocation allocation, PaymentRefundRequest request,
                                    PaymentFailureCode failureCode, boolean retryable, String reason) {
        // 보상 워크플로우가 이어질 수 있도록 실패 이벤트 비동기 발행
        eventPublisher.publish(new RefundFailedEvent(
                payment.getOrderUuid(),
                payment.getUuid(),
                payment.getUserUuid(),
                request.getRefundAmount(),
                allocation.pgRefundAmount(),
                allocation.depositRefundAmount(),
                failureCode,
                retryable,
                reason,
                LocalDateTime.now()));
    }

    private String buildFailureReason(PaymentFailureCode code, String detail) {
        if (detail == null || detail.isBlank()) {
            return code.name();
        }
        return code.name() + ": " + detail;
    }

    private record RefundAllocation(long pgRefundAmount, long depositRefundAmount,
                                   List<ItemRefundAllocation> itemRefundAllocations) {
    }

    private record ItemRefundAllocation(PaymentOrderItem paymentOrderItem, long refundAmount, long depositAmount,
                                       long pgAmount) {
    }
}
