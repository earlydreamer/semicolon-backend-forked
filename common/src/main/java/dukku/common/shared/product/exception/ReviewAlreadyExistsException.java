package dukku.common.shared.product.exception;

public class ReviewAlreadyExistsException extends ProductBadRequestException {
    public ReviewAlreadyExistsException() {
        super("이미 해당 상품에 대한 리뷰를 작성하셨습니다.");
    }
}
