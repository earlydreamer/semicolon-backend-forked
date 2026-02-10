package dukku.semicolon.boundedContext.product.app.usecase.review;

import dukku.semicolon.boundedContext.product.app.support.SellerReviewMapper;
import dukku.semicolon.boundedContext.product.entity.SellerReview;
import dukku.semicolon.boundedContext.product.out.SellerReviewRepository;
import dukku.common.shared.product.dto.review.SellerReviewListResponse;
import dukku.common.shared.product.dto.review.SellerReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindSellerReviewListUseCase {

    private final SellerReviewRepository sellerReviewRepository;

    @Transactional(readOnly = true)
    public SellerReviewListResponse execute(UUID sellerUuid, Pageable pageable) {

        Page<SellerReview> page = sellerReviewRepository.findBySellerUuid(sellerUuid, pageable);

        List<SellerReviewResponse> items = page.getContent().stream()
                .map(SellerReviewMapper::toResponse)
                .toList();

        return SellerReviewListResponse.builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalCount(page.getTotalElements())
                .hasNext(page.hasNext())
                .build();
    }
}
