package dukku.payment.boundedContext.payment.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.common.shared.coupon.exception.CouponUseNotAllowedException;
import dukku.common.shared.coupon.out.CouponApiClient;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.deposit.out.depositApiClient.DepositApiClient;
import dukku.common.shared.payment.dto.PaymentRequest;
import dukku.common.shared.payment.dto.PaymentResponse;
import dukku.common.shared.payment.exception.AmountMismatchException;
import dukku.common.shared.payment.exception.DepositShortageException;
import dukku.common.shared.payment.type.PaymentHistoryType;
import dukku.common.shared.payment.type.PaymentType;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.payment.boundedContext.payment.entity.PaymentOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 결제 요청(준비) UseCase
 *
 * <p>
 * 상품 총액 서버 재계산, 쿠폰 비례 분배, 금액 유효성 검증 후
 * {@link Payment} 엔티티 생성 및 요청 이력 기록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestPaymentUseCase {

    @Value("${toss.callback.base-url}")
    private String tossCallbackBaseUrl;

    private final PaymentSupport support;
    private final DepositApiClient depositApiClient;
    private final CouponApiClient couponApiClient;

    /**
     * 결제 요청(준비) 실행
     *
     * <p>
     * 서버 측 상품 총액 재계산, 쿠폰 비례 분배, 금액 일관성 검증 후
     * {@link Payment} 엔티티 저장 및 요청 이력 생성
     *
     * @param request        결제 요청 DTO
     * @param idempotencyKey 멱등성 키 (중복 요청 방지용)
     * @return 생성된 결제 응답 DTO
     * @throws AmountMismatchException      상품 총액 불일치, 쿠폰 초과, 분배 오류 등
     * @throws CouponUseNotAllowedException 비활성 쿠폰 사용 시도 시
     * @throws dukku.common.shared.payment.exception.DepositShortageException 예치금 잔액 부족 시
     */
    public PaymentResponse execute(PaymentRequest request, String idempotencyKey) {
        log.debug("결제 요청 처리 시작. orderUuid={}, idempotencyKey={}", request.getOrderUuid(), idempotencyKey);

        // 1. 멱등성 검증
        Optional<PaymentResponse> cachedResponse = checkIdempotency(request.getOrderUuid(), idempotencyKey);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        // 2. 정렬 + 서버 상품총액 계산/검증
        List<PaymentRequest.PaymentRequestItem> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(PaymentRequest.PaymentRequestItem::getProductId)
                        .thenComparing(PaymentRequest.PaymentRequestItem::getOrderItemUuid))
                .toList();

        long serverItemsTotalAmount = calculateServerItemsTotalAmount(sortedItems);
        validateItemsTotalAmount(request.getAmounts(), serverItemsTotalAmount);

        // 3. 쿠폰 분배 (서버 계산)
        long[] itemCouponAmounts = calculateCouponDistribution(
                request.getCouponUuid(),
                sortedItems,
                request.getAmounts(),
                serverItemsTotalAmount);

        // 4. 금액 유효성 검증
        validateAmounts(request.getAmounts());

        // 5. Payment 생성 및 스냅샷 저장
        String tossOrderId = generateTossOrderId(UUID.randomUUID());
        Payment payment = createPayment(request, tossOrderId, sortedItems, itemCouponAmounts);

        // 5-1. 결제 요청 이력
        support.createHistory(payment, PaymentHistoryType.PAYMENT_REQUESTED, null, 0L, 0L);

        // 6. 응답 생성
        log.debug("결제 요청 완료. orderUuid={}, tossOrderId={}", request.getOrderUuid(), tossOrderId);
        return payment.toPaymentResponse(request.getOrderName(), tossCallbackBaseUrl);
    }

    /**
     * 정렬된 상품 목록으로부터 서버 측 상품 총액 계산
     *
     * @param sortedItems 정렬된 상품 목록
     * @return 상품 가격 합계 (원)
     * @throws AmountMismatchException 상품 목록이 비어 있을 때
     */
    private long calculateServerItemsTotalAmount(List<PaymentRequest.PaymentRequestItem> sortedItems) {
        if (sortedItems.isEmpty()) {
            throw new AmountMismatchException("결제 상품이 비어 있습니다.");
        }
        return sortedItems.stream()
                .mapToLong(PaymentRequest.PaymentRequestItem::getPrice)
                .sum();
    }

    /**
     * 요청 상품 총액과 서버 재계산 총액 일치 여부 검증
     *
     * @param amounts                요청 금액 정보
     * @param serverItemsTotalAmount 서버 재계산 상품 총액
     * @throws AmountMismatchException 총액 불일치 시
     */
    private void validateItemsTotalAmount(PaymentRequest.Amounts amounts, long serverItemsTotalAmount) {
        if (!amounts.getItemsTotalAmount().equals(serverItemsTotalAmount)) {
            throw new AmountMismatchException(
                    "상품총액 불일치: 요청값=" + amounts.getItemsTotalAmount()
                            + ", 서버계산값=" + serverItemsTotalAmount);
        }
    }

    /**
     * 쿠폰 할인액을 상품별로 비례 분배한다.
     * 규칙: 중간 상품 floor, 마지막 상품이 오차 흡수.
     * 안전장치: 마지막 상품이 상품금액을 넘으면 앞 상품으로 역재분배.
     */
    private long[] calculateCouponDistribution(UUID couponUuid,
                                               List<PaymentRequest.PaymentRequestItem> sortedItems,
                                               PaymentRequest.Amounts amounts,
                                               long serverItemsTotalAmount) {
        long[] itemCouponAmounts = new long[sortedItems.size()];

        if (couponUuid == null) {
            amounts.setCouponDiscountAmount(0L);
            return itemCouponAmounts;
        }

        CouponInternalResponse coupon = couponApiClient.getCouponInfo(couponUuid);

        if (coupon.status() != CouponStatus.ACTIVE) {
            throw new CouponUseNotAllowedException();
        }

        if (serverItemsTotalAmount < coupon.minimumOrderAmount()) {
            throw new AmountMismatchException(
                    "최소 주문 금액 미달: 상품총액=" + serverItemsTotalAmount
                            + ", 최소주문금액=" + coupon.minimumOrderAmount());
        }

        long couponTotal = coupon.discountAmount();
        if (couponTotal > serverItemsTotalAmount) {
            throw new AmountMismatchException(
                    "쿠폰 할인액이 상품총액을 초과합니다. coupon=" + couponTotal
                            + ", itemsTotal=" + serverItemsTotalAmount);
        }

        amounts.setCouponDiscountAmount(couponTotal);
        log.debug("쿠폰 비례 분배 시작. couponUuid={}, couponTotal={}, itemCount={}",
                couponUuid, couponTotal, sortedItems.size());

        long distributed = 0;
        for (int i = 0; i < sortedItems.size() - 1; i++) {
            long itemPrice = sortedItems.get(i).getPrice();
            long itemCoupon = couponTotal * itemPrice / serverItemsTotalAmount;
            itemCouponAmounts[i] = itemCoupon;
            distributed += itemCoupon;
        }

        int lastIndex = sortedItems.size() - 1;
        itemCouponAmounts[lastIndex] = couponTotal - distributed;

        rebalanceLastItemOverflow(itemCouponAmounts, sortedItems);
        validateCouponDistribution(itemCouponAmounts, sortedItems, couponTotal);

        log.debug("쿠폰 비례 분배 완료. couponTotal={}", couponTotal);
        return itemCouponAmounts;
    }

    /**
     * 마지막 상품 쿠폰 할당액 초과 시 앞 상품으로 재분배
     *
     * <p>
     * 비례 분배 시 마지막 상품이 잔여 오차를 흡수하는 과정에서 상품 가격을 초과할 수 있으며,
     * 앞 상품들의 여유 금액(price - 현재 쿠폰)으로 순차 이전
     *
     * @param itemCouponAmounts 상품별 쿠폰 할당액 배열 (수정됨)
     * @param sortedItems       정렬된 상품 목록
     * @throws AmountMismatchException 재분배 후에도 초과분이 남아 있을 때
     */
    private void rebalanceLastItemOverflow(long[] itemCouponAmounts,
                                           List<PaymentRequest.PaymentRequestItem> sortedItems) {
        int lastIndex = sortedItems.size() - 1;
        long lastItemPrice = sortedItems.get(lastIndex).getPrice();
        long lastItemCoupon = itemCouponAmounts[lastIndex];
        if (lastItemCoupon <= lastItemPrice) {
            return;
        }

        long overflow = lastItemCoupon - lastItemPrice;
        itemCouponAmounts[lastIndex] = lastItemPrice;

        for (int i = 0; i < lastIndex && overflow > 0; i++) {
            long itemPrice = sortedItems.get(i).getPrice();
            long expandable = itemPrice - itemCouponAmounts[i];
            if (expandable <= 0) {
                continue;
            }
            long transfer = Math.min(expandable, overflow);
            itemCouponAmounts[i] += transfer;
            overflow -= transfer;
        }

        if (overflow > 0) {
            throw new AmountMismatchException("쿠폰 할인액 재분배 실패: 남은 오차=" + overflow);
        }
    }

    /**
     * 쿠폰 분배 결과 정합성 검증
     *
     * <p>
     * 검증 항목:
     * <ul>
     *   <li>각 상품 쿠폰 할당액이 음수가 아닌지</li>
     *   <li>각 상품 쿠폰 할당액이 상품 가격을 초과하지 않는지</li>
     *   <li>전체 분배 합계가 쿠폰 총액과 일치하는지</li>
     * </ul>
     *
     * @param itemCouponAmounts 상품별 쿠폰 할당액 배열
     * @param sortedItems       정렬된 상품 목록
     * @param couponTotal       쿠폰 총 할인액
     * @throws AmountMismatchException 분배 결과가 유효하지 않을 때
     */
    private void validateCouponDistribution(long[] itemCouponAmounts,
                                            List<PaymentRequest.PaymentRequestItem> sortedItems,
                                            long couponTotal) {
        long distributedSum = 0;
        for (int i = 0; i < sortedItems.size(); i++) {
            long itemCoupon = itemCouponAmounts[i];
            long itemPrice = sortedItems.get(i).getPrice();
            if (itemCoupon < 0) {
                throw new AmountMismatchException("쿠폰 분배 금액이 음수입니다. index=" + i + ", coupon=" + itemCoupon);
            }
            if (itemCoupon > itemPrice) {
                throw new AmountMismatchException(
                        "상품 금액을 초과한 쿠폰 분배가 발생했습니다. index=" + i
                                + ", coupon=" + itemCoupon + ", price=" + itemPrice);
            }
            distributedSum += itemCoupon;
        }

        if (distributedSum != couponTotal) {
            throw new AmountMismatchException(
                    "쿠폰 분배 합계 불일치: expected=" + couponTotal + ", actual=" + distributedSum);
        }
    }

    /**
     * 멱등성 키 기반 중복 요청 여부 확인
     *
     * <p>
     * 동일 키로 이미 처리된 결제가 있으면 캐시된 응답 반환
     *
     * @param orderUuid      주문 UUID
     * @param idempotencyKey 멱등성 키
     * @return 이전에 처리된 응답이 있으면 {@link Optional}에 래핑, 없으면 빈 Optional
     */
    private Optional<PaymentResponse> checkIdempotency(UUID orderUuid, String idempotencyKey) {
        // TODO: Phase 2 - Redis 또는 DB 기반 멱등성 검증 구현
        return Optional.empty();
    }

    /**
     * 결제 금액 일관성 및 예치금 잔액 검증
     *
     * <p>
     * 검증 항목:
     * <ul>
     *   <li>모든 금액이 0 이상인지</li>
     *   <li>{@code finalPayAmount == depositUseAmount + pgPayAmount}</li>
     *   <li>{@code finalPayAmount == itemsTotalAmount - couponDiscountAmount}</li>
     *   <li>예치금 사용액이 잔액 이하인지</li>
     * </ul>
     *
     * @param amounts 결제 요청 금액 정보
     * @throws AmountMismatchException   금액 계산 불일치 시
     * @throws dukku.common.shared.payment.exception.DepositShortageException 예치금 잔액 부족 시
     */
    private void validateAmounts(PaymentRequest.Amounts amounts) {
        // 1. 음수 금액 체크
        if (amounts.getFinalPayAmount() < 0
                || amounts.getDepositUseAmount() < 0
                || amounts.getPgPayAmount() < 0
                || amounts.getCouponDiscountAmount() < 0) {
            throw new AmountMismatchException("결제 금액은 0 이상이어야 합니다.");
        }

        // 2. finalPayAmount = depositUseAmount + pgPayAmount
        Long expectedFinalAmount = amounts.getDepositUseAmount() + amounts.getPgPayAmount();
        if (!expectedFinalAmount.equals(amounts.getFinalPayAmount())) {
            throw new AmountMismatchException(
                    "최종 결제금액 불일치: 예상=" + expectedFinalAmount
                            + " (예치금 " + amounts.getDepositUseAmount()
                            + " + PG " + amounts.getPgPayAmount() + ")"
                            + ", 실제=" + amounts.getFinalPayAmount());
        }

        // 3. itemsTotalAmount - couponDiscountAmount = finalPayAmount
        Long expectedFromItems = amounts.getItemsTotalAmount() - amounts.getCouponDiscountAmount();
        if (!expectedFromItems.equals(amounts.getFinalPayAmount())) {
            throw new AmountMismatchException(
                    "상품금액 계산 불일치: 예상=" + expectedFromItems
                            + " (상품총액 " + amounts.getItemsTotalAmount()
                            + " - 쿠폰할인 " + amounts.getCouponDiscountAmount() + ")"
                            + ", 실제=" + amounts.getFinalPayAmount());
        }

        // 4. 예치금 잔액 검증
        Long depositUseAmount = amounts.getDepositUseAmount();
        if (depositUseAmount != null && depositUseAmount > 0) {
            Long availableBalance = depositApiClient.getBalance(UserUtil.getUserId());
            if (availableBalance == null || availableBalance < depositUseAmount) {
                throw new DepositShortageException(depositUseAmount,
                        availableBalance != null ? availableBalance : 0L);
            }
        }
    }

    /**
     * {@link Payment} 엔티티와 상품 스냅샷 항목 생성 및 저장
     *
     * <p>
     * PG 결제 금액이 0이면 {@link dukku.common.shared.payment.type.PaymentType#DEPOSIT} (예치금 전액),
     * 그 외에는 {@link dukku.common.shared.payment.type.PaymentType#MIXED} 설정.
     * 예치금은 상품 순서대로 순차 차감
     *
     * @param request           결제 요청 DTO
     * @param tossOrderId       토스 주문 ID
     * @param sortedItems       정렬된 상품 목록
     * @param itemCouponAmounts 상품별 쿠폰 할당액 배열
     * @return 저장된 {@link Payment} 엔티티
     * @throws AmountMismatchException 상품별 결제 대상 금액이 음수일 때
     */
    private Payment createPayment(PaymentRequest request, String tossOrderId,
                                  List<PaymentRequest.PaymentRequestItem> sortedItems,
                                  long[] itemCouponAmounts) {
        UUID userUuid = UserUtil.getUserId();
        PaymentRequest.Amounts amounts = request.getAmounts();

        PaymentType paymentType = amounts.getPgPayAmount() == 0
                ? PaymentType.DEPOSIT
                : PaymentType.MIXED;

        Payment payment = Payment.create(
                request.getOrderUuid(),
                userUuid,
                amounts.getFinalPayAmount(),
                amounts.getDepositUseAmount(),
                amounts.getPgPayAmount(),
                amounts.getCouponDiscountAmount(),
                paymentType,
                tossOrderId,
                request.getCouponUuid());

        Long remainingDeposit = amounts.getDepositUseAmount();

        for (int i = 0; i < sortedItems.size(); i++) {
            PaymentRequest.PaymentRequestItem itemDto = sortedItems.get(i);
            long itemCoupon = itemCouponAmounts[i];

            Long itemPayableAmount = itemDto.getPrice() - itemCoupon;
            if (itemPayableAmount < 0) {
                throw new AmountMismatchException(
                        "상품별 결제 대상 금액이 음수입니다. orderItemUuid=" + itemDto.getOrderItemUuid()
                                + ", price=" + itemDto.getPrice() + ", coupon=" + itemCoupon);
            }

            Long itemDepositUse = Math.min(remainingDeposit, itemPayableAmount);
            remainingDeposit -= itemDepositUse;

            payment.addItem(PaymentOrderItem.create(
                    payment,
                    request.getOrderUuid(),
                    itemDto.getOrderItemUuid(),
                    itemDto.getProductId(),
                    itemDto.getProductName(),
                    itemDto.getPrice(),
                    itemCoupon,
                    itemDto.getSellerUuid(),
                    itemDepositUse));
        }

        return support.savePayment(payment);
    }

    /**
     * 토스 결제 주문 ID 생성
     *
     * <p>
     * 형식: {@code TOSS_{UUID 앞 8자리}_{yyyyMMdd}}
     *
     * @param paymentUuid 결제 UUID
     * @return 토스 주문 ID 문자열
     */
    private String generateTossOrderId(UUID paymentUuid) {
        String datePart = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        return "TOSS_" + paymentUuid.toString().substring(0, 8) + "_" + datePart;
    }
}
