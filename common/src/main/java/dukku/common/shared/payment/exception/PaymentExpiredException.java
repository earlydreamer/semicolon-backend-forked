package dukku.common.shared.payment.exception;

import dukku.common.global.exception.ConflictException;

public class PaymentExpiredException extends ConflictException {
    public PaymentExpiredException() {
        super("결제 가능한 시간이 만료되었습니다.");
    }

    public PaymentExpiredException(Long orderAgeMinutes, Long allowedMinutes) {
        super(String.format("결제 가능 시간이 만료되었습니다. (주문 경과: %d분, 허용: %d분)", orderAgeMinutes, allowedMinutes));
    }
}
