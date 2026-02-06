package dukku.semicolon.boundedContext.product.out;

import dukku.semicolon.shared.product.dto.cqrs.SellerReviewStatDto;

import java.util.List;

public interface CustomProductSellerRepository {
    int[] bulkUpdateReviewSummary(List<SellerReviewStatDto> items);
}
