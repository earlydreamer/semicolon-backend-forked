package dukku.product.boundedContext.product.app.support;

import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductImage;
import dukku.product.boundedContext.product.entity.ProductSeller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class ProductMapper {

    // 리스트 조회용 DTO 매핑(태그 기본 적용)
    public static ProductListItemResponse toListItem(Product p) {
        return toListItem(p, p.getTagNames());
    }

    // 리스트 조회용 DTO 매핑(태그 목록 override)
    public static ProductListItemResponse toListItem(Product p, List<String> tagNames) {
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
                .tagNames(tagNames)
                .build();
    }

    // 상세 정보 매핑 (기본 스펙)
    public static ProductDetailResponse toDetail(Product p) {
        return toDetail(p, null, null);
    }

    // 상세 정보 매핑 (판매자/닉네임 포함)
    public static ProductDetailResponse toDetail(Product p, ProductSeller seller, String nickname) {
        return toDetail(p, seller, nickname, null, null);
    }

    // 상세 정보 매핑 (평점/리뷰 수 포함)
    public static ProductDetailResponse toDetail(
            Product p,
            ProductSeller seller,
            String nickname,
            BigDecimal averageRating,
            Integer reviewCount
    ) {
        return toDetail(p, seller, nickname, averageRating, reviewCount, null, null);
    }

    // 상세 정보 매핑 (좋아요/조회수 override 허용)
    public static ProductDetailResponse toDetail(
            Product p,
            ProductSeller seller,
            String nickname,
            BigDecimal averageRating,
            Integer reviewCount,
            Integer likeCount,
            Integer viewCount
    ) {
        List<String> imageUrls = p.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(ProductImage::getImageUrl)
                .toList();

        BigDecimal resolvedAverageRating = averageRating != null
                ? averageRating
                : seller == null ? null : seller.getAverageRating();
        int resolvedReviewCount = reviewCount != null
                ? reviewCount
                : seller == null ? 0 : seller.getReviewCount();

        return ProductDetailResponse.builder()
                .productId(p.getId())
                .productUuid(p.getUuid())
                .sellerUuid(p.getSellerUuid())
                .title(p.getTitle())
                .description(p.getDescription())
                .price(p.getPrice())
                .shippingFee(p.getShippingFee())
                .likeCount(likeCount != null ? likeCount : p.getLikeCount())
                .viewCount(viewCount != null ? viewCount : p.getViewCount())
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
                        .shopUuid(seller.getUuid())
                        .sellerUuid(seller.getSellerUuid())
                        .nickname(nickname)
                        .averageRating(resolvedAverageRating)
                        .reviewCount(resolvedReviewCount)
                        .build())
                .createdAt(p.getCreatedAt())
                .tagNames(p.getTagNames())
                .build();
    }

    // 공유 URL 생성용 기본 도메인 조립
    public static String buildProductUrl(java.util.UUID productUuid) {
        String base = System.getenv("FRONTEND_BASE_URL");
        if (base == null || base.isBlank()) {
            base = System.getProperty("frontend.base.url");
        }
        if (base == null || base.isBlank()) {
            base = "https://dukku.shop";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/products/" + productUuid;
    }
}
