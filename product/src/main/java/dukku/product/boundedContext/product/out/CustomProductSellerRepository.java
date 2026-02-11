package dukku.product.boundedContext.product.out;

import dukku.common.shared.product.dto.cqrs.SellerReviewStatDto;

import java.util.List;

public interface CustomProductSellerRepository {
    int[] bulkUpdateReviewSummary(List<SellerReviewStatDto> items);
}
