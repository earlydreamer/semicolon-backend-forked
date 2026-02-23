package dukku.product.boundedContext.product.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.product.dto.shop.ShopResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "product_sellers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_sellers_user_uuid", columnNames = {"user_uuid"}),
                @UniqueConstraint(name = "uk_product_sellers_seller_uuid", columnNames = {"seller_uuid"})
        }
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSeller extends BaseIdAndUUIDAndTime {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "seller_uuid", nullable = false, columnDefinition = "uuid", comment = "판매자 UUID")
    private UUID sellerUuid;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_uuid", nullable = false, columnDefinition = "uuid", comment = "유저 UUID")
    private UUID userUuid;

    @Column(columnDefinition = "TEXT", comment = "상점 소개")
    private String intro;

    @Column(nullable = false, comment = "누적 판매 횟수")
    private int salesCount;

    @Column(nullable = false, comment = "현재 판매중 상품 수")
    private int activeListingCount;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2, comment = "평균 평점")
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false, comment = "리뷰 수")
    private int reviewCount;

    public static ProductSeller create(UUID userUuid, String intro, int salesCount, int activeListingCount) {
        return ProductSeller.builder()
                .sellerUuid(userUuid)
                .userUuid(userUuid)
                .intro(intro)
                .salesCount(salesCount)
                .activeListingCount(activeListingCount)
                .averageRating(BigDecimal.ZERO) // 0.00
                .reviewCount(0)
                .build();
    }

    public static ProductSeller create(UUID userUuid, String intro) {
        return create(userUuid, intro, 0, 0);
    }

    public void changeIntro(String intro) {
        this.intro = intro;
    }

    public static ShopResponse from(ProductSeller seller, String nickname) {
        return ShopResponse.builder()
                .shopUuid(seller.getUuid())
                .nickname(nickname)
                .intro(seller.getIntro())
                .salesCount(seller.getSalesCount())
                .activeListingCount(seller.getActiveListingCount())
                .averageRating(seller.getAverageRating())
                .reviewCount(seller.getReviewCount())
                .build();
    }
}
