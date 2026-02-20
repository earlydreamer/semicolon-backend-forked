package dukku.common.shared.product.exception;

public class CartSelfProductNotAllowedException extends ProductBadRequestException {
    public CartSelfProductNotAllowedException() {
        super("자신의 상품은 장바구니에 담을 수 없습니다.");
    }
}
