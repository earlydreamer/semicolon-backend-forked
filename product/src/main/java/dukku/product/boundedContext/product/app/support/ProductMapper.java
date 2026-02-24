package dukku.product.boundedContext.product.app.support;

import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductImage;
import dukku.product.boundedContext.product.entity.ProductSeller;

import java.util.Comparator;
import java.util.List;

public class ProductMapper {

    public static ProductListItemResponse toListItem(Product p) {
        String thumb = p.getImages().stream().min(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductListItemResponse.builder()
                .productUuid(p.getUuid())
                .title(p.getTitle())
                .price(p.getPrice())
                .thumbnailUrl(thumb)
                .saleStatus(p.getSaleStatus())
                .createdAt(p.getCreatedAt())
                .likeCount(p.getLikeCount())
                .viewCount(p.getViewCount())
                .commentCount(p.getCommentCount())
                .tagNames(p.getTagNames())
                .build();
    }

    public static ProductDetailResponse toDetail(Product p) {
        return toDetail(p, null, null);
    }

    public static ProductDetailResponse toDetail(Product p, ProductSeller seller, String nickname) {
        List<String> imageUrls = p.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(ProductImage::getImageUrl)
                .toList();

        return ProductDetailResponse.builder()
                .productId(p.getId())
                .productUuid(p.getUuid())
                .sellerUuid(p.getSellerUuid())
                .title(p.getTitle())
                .description(p.getDescription())
                .price(p.getPrice())
                .shippingFee(p.getShippingFee())
                .likeCount(p.getLikeCount())
                .viewCount(p.getViewCount())
                .imageUrls(imageUrls)
                .conditionStatus(p.getConditionStatus())
                .saleStatus(p.getSaleStatus())
                .visibilityStatus(p.getVisibilityStatus())
                .category(ProductDetailResponse.CategorySummary.builder()
                        .id(p.getCategory().getId())
                        .name(p.getCategory().getCategoryName())
                        .depth(p.getCategory().getDepth())
                        .build())
                .seller(seller == null ? null : ProductDetailResponse.Seller.builder()
                        .sellerUuid(seller.getUserUuid())
                        .nickname(nickname)
                        .averageRating(seller.getAverageRating())
                        .reviewCount(seller.getReviewCount())
                        .build())
                .createdAt(p.getCreatedAt())
                .tagNames(p.getTagNames())
                .build();
    }

    // 환경 변수 FRONTEND_BASE_URL 또는 시스템 속성 frontend.base.url을 사용해
    // 상품 상세 페이지 URL을 생성합니다.
    // 예: https://dukku.shop/products/{productUuid}
    public static String buildProductUrl(java.util.UUID productUuid) {
        String base = System.getenv("FRONTEND_BASE_URL");
        if (base == null || base.isBlank()) {
            base = System.getProperty("frontend.base.url");
        }
        if (base == null || base.isBlank()) {
            base = "https://dukku.shop"; // 기본 폴백
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/products/" + productUuid.toString();
    }
}
