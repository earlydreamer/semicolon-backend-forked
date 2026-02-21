package dukku.settlement.boundedContext.settlement.batch.notification;

import dukku.common.shared.settlement.exception.SettlementProcessingException;
import dukku.common.shared.settlement.exception.SettlementValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Skip 사유 유형 분류
 * - Skip된 예외를 분석하여 사람이 읽기 쉬운 사유로 분류
 */
@Getter
@RequiredArgsConstructor
public enum SkipReasonType {

    DEPOSIT_ACCOUNT_NOT_FOUND("예치금 계좌 미존재"),
    PAYMENT_NOT_FOUND("결제 정보 미존재"),
    DEPOSIT_CHARGE_FAILED("예치금 충전 실패"),
    INVALID_AMOUNT("금액 유효성 오류"),
    STATUS_TRANSITION_FAILED("상태 전이 불가"),
    MISSING_REQUIRED_FIELD("필수 값 누락"),
    VALIDATION_ERROR("데이터 검증 오류"),
    PROCESSING_ERROR("처리 오류"),
    UNKNOWN("알 수 없는 오류");

    private final String description;

    /**
     * 예외를 분석하여 Skip 사유 유형을 반환
     */
    public static SkipReasonType classify(Throwable t) {
        String message = t.getMessage() != null ? t.getMessage() : "";

        if (message.contains("예치금 계좌")) return DEPOSIT_ACCOUNT_NOT_FOUND;
        if (message.contains("결제 정보")) return PAYMENT_NOT_FOUND;
        if (message.contains("예치금 충전")) return DEPOSIT_CHARGE_FAILED;
        if (message.contains("금액이 유효하지 않습니다")) return INVALID_AMOUNT;
        if (message.contains("상태 전이")) return STATUS_TRANSITION_FAILED;
        if (message.contains("필수 값")) return MISSING_REQUIRED_FIELD;

        if (t instanceof SettlementValidationException) return VALIDATION_ERROR;
        if (t instanceof SettlementProcessingException) return PROCESSING_ERROR;

        return UNKNOWN;
    }
}
