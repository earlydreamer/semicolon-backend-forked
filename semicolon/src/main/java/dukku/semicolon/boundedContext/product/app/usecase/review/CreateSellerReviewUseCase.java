package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.app.support.SellerReviewMapper;
import dukku.semicolon.boundedContext.product.entity.SellerReview;
import dukku.semicolon.boundedContext.product.out.SellerReviewRepository;
import dukku.semicolon.shared.product.dto.review.SellerReviewCreateRequest;
import dukku.semicolon.shared.product.dto.review.SellerReviewResponse;
import dukku.semicolon.shared.product.exception.ReviewAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateSellerReviewUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    // TODO : FeignClient 작성 예정
    @Transactional
    public SellerReviewResponse execute(UUID buyerUuid, SellerReviewCreateRequest request) {

        // 1) (Feign 붙이기 전이라 가정) orderItemUuid 중복 리뷰 방지 + 삭제 시 재작성은 허용
        if (sellerReviewRepository.existsByOrderItemUuidAndDeletedAtIsNull(request.getOrderItemUuid())) {
            throw new ReviewAlreadyExistsException();
        }

        // 2) 저장
        SellerReview saved = sellerReviewRepository.save(
                SellerReview.create(
                        request.getSellerUuid(),
                        buyerUuid,
                        request.getOrderItemUuid(),
                        request.getProductUuid(),
                        request.getRating(),
                        request.getContent()
                )
        );

        return SellerReviewMapper.toResponse(saved);
    }
}
