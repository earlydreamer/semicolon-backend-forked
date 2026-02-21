package dukku.product.boundedContext.product.entity;

import dukku.common.global.jpa.entity.BaseIdAndTime;
import dukku.common.shared.product.dto.cart.CartDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Comparator;

@Entity
@Table(
        name = "carts",
        uniqueConstraints = {
                // 한 유저는 동일한 상품을 중복해서 담을 수 없음
                @UniqueConstraint(
                        name = "uk_carts_user_product",
                        columnNames = {"user_uuid", "product_id"}
                )
        },
        indexes = {
                // 특정 유저의 장바구니 조회 성능 향상
                @Index(name = "idx_carts_user_uuid", columnList = "user_uuid")
        }
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseIdAndTime {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_uuid", nullable = false, comment = "구매 희망자 (ProductUser 참조)")
    private ProductUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, comment = "상품 ID")
    private Product product;

    public static Cart createCart(ProductUser user, Product product) {
        return Cart.builder()
                .user(user)
                .product(product)
                .build();
    }

    public static CartDto toDto(Cart cart) {
        Product product = cart.getProduct(); // 지연 로딩 없이 바로 접근 가능

        /*
        썸네일 이미지 결정 로직
        1순위: isThumbnail = true인 이미지
        2순위: sortOrder가 가장 낮은(1번) 이미지
        3순위: null (이미지가 아예 없을 때)
        */
        String thumbnailUrl = product.getImages().stream()
                .filter(ProductImage::isThumbnail)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() ->
                        product.getImages().stream()
                                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                                .map(ProductImage::getImageUrl)
                                .orElse(null)
                );

        return new CartDto(
                cart.getId(),
                product.getUuid(),
                product.getSellerUuid(),
                product.getTitle(),
                product.getPrice(),
                product.getSaleStatus(),
                thumbnailUrl,
                cart.getCreatedAt()
        );
    }
}
