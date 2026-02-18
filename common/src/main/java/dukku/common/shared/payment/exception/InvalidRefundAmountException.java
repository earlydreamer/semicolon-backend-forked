package dukku.common.shared.payment.exception;

import dukku.common.global.exception.BadRequestException;

/**
 * Raised when refund amount validation fails.
 */
public class InvalidRefundAmountException extends BadRequestException {

    private InvalidRefundAmountException(String details) {
        super(details);
    }

    public static InvalidRefundAmountException invalid() {
        return new InvalidRefundAmountException("유효하지 않은 환불 금액입니다.");
    }

    public static InvalidRefundAmountException orderMismatch() {
        return new InvalidRefundAmountException("환불 요청 주문 UUID가 결제 주문과 일치하지 않습니다.");
    }

    public static InvalidRefundAmountException exceedsAvailable(Long requested, Long available) {
        return new InvalidRefundAmountException(
                String.format("환불 금액 초과: 요청(%d원), 가능(%d원)", requested, available)
        );
    }

    public static InvalidRefundAmountException itemTotalMismatch() {
        return new InvalidRefundAmountException("요청 항목 금액 합계가 총 환불 금액과 일치하지 않습니다.");
    }

    public static InvalidRefundAmountException itemRequestInvalid() {
        return new InvalidRefundAmountException("환불 항목의 주문 상품 UUID/금액이 비어 있습니다.");
    }

    public static InvalidRefundAmountException duplicateOrderItem() {
        return new InvalidRefundAmountException("동일한 주문 상품이 중복 등록되었습니다.");
    }

    public static InvalidRefundAmountException orderItemNotFound() {
        return new InvalidRefundAmountException("환불 요청한 주문 상품을 찾을 수 없습니다.");
    }

    public static InvalidRefundAmountException paymentAmountCorrupted() {
        return new InvalidRefundAmountException("상품 결제 금액 계산이 잘못되었습니다.");
    }

    public static InvalidRefundAmountException itemExceedsAvailable(Long requested, Long available) {
        return new InvalidRefundAmountException(
                String.format("요청 환불 금액이 상품 환불 가능 금액을 초과합니다: 요청(%d원), 가능(%d원)",
                        requested, available)
        );
    }

    public static InvalidRefundAmountException itemDepositCorrupted() {
        return new InvalidRefundAmountException("상품 환불된 예치금이 잘못되어 계산할 수 없습니다.");
    }
}
