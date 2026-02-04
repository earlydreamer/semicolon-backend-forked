package dukku.semicolon.shared.product.dto.review;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SellerReviewListResponse {
    private List<SellerReviewResponse> items;
    private int page;
    private int size;
    private long totalCount;
    private boolean hasNext;

    private double avgRating; // 평균 평점
    private long reviewCount;
}
