package dukku.common.shared.order.exception;

import dukku.common.global.exception.BadRequestException;

public class OrderDateRangeInvalidException extends BadRequestException {
    public OrderDateRangeInvalidException() {
        super("조회 시작일은 종료일보다 이전이어야 합니다.");
    }
}
