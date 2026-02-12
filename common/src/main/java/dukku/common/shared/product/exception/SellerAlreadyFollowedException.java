package dukku.common.shared.product.exception;

import dukku.common.global.exception.ConflictException;

public class SellerAlreadyFollowedException extends ConflictException {
    public SellerAlreadyFollowedException() {
        super("이미 팔로우한 판매자입니다.");
    }
}
