package dukku.common.shared.order.exception;

import dukku.common.global.exception.ConflictException;
import dukku.common.shared.order.type.ReturnStatus;

/**
 * 반품 상태 전이 불가 예외
 */
public class ReturnRequestStatusInvalidException extends ConflictException {

    /**
     * 현재 상태 기준 허용되지 않은 반품 처리 예외 생성
     */
    public ReturnRequestStatusInvalidException(ReturnStatus currentStatus, String requiredStateDescription) {
        super("현재 반품 상태(" + currentStatus + ")에서는 요청을 처리할 수 없습니다. 필요 상태: " + requiredStateDescription);
    }
}
