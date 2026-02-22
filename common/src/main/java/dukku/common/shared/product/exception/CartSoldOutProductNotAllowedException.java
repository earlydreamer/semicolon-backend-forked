package dukku.common.shared.product.exception;

public class CartSoldOutProductNotAllowedException extends ProductBadRequestException {
    public CartSoldOutProductNotAllowedException() {
        super("판매 완료된 상품은 장바구니에 담을 수 없습니다.");
    }
}
