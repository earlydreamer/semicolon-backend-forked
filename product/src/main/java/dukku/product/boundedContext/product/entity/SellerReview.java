package dukku.product.boundedContext.product.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "seller_reviews",
        indexes = {
                @Index(name = "idx_seller_reviews_seller_created", columnList = "seller_uuid, created_at"),
                @Index(name = "idx_product_reviews_product_created", columnList = "product_uuid, created_at")
        }
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerReview extends BaseIdAndUUIDAndTime {

    // 리뷰 대상 판매자 UUID
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "seller_uuid", nullable = false, columnDefinition = "uuid")
    private UUID sellerUuid;

    // 리뷰 작성자(구매자) UUID - DB 상에서는 user_uuid
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_uuid", nullable = false, columnDefinition = "uuid")
    private UUID buyerUuid;

    // 리뷰 대상 주문 상품 UUID
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "order_item_uuid", nullable = false, columnDefinition = "uuid")
    private UUID orderItemUuid;

    // 리뷰 대상 상품 UUID
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "product_uuid", nullable = false, columnDefinition = "uuid")
    private UUID productUuid;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating; // 1~5

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime deletedAt;

    public static SellerReview create(UUID sellerUuid, UUID buyerUuid, UUID orderItemUuid, UUID productUuid, int rating, String content) {
        return SellerReview.builder()
                .sellerUuid(sellerUuid)
                .buyerUuid(buyerUuid)
                .orderItemUuid(orderItemUuid)
                .productUuid(productUuid)
                .rating(rating)
                .content(content)
                .build();
    }

    public void softDelete() { this.deletedAt = LocalDateTime.now(); }

    public boolean isDeleted() { return deletedAt != null; }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeRating(int rating) {
        this.rating = rating;
    }
}
