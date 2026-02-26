package dukku.product.boundedContext.product.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import dukku.common.shared.product.dto.product.ProductListResponse;
import dukku.common.shared.product.dto.product.ProductPayload;
import dukku.common.shared.product.exception.ProductReservationConflictException;
import dukku.common.shared.product.type.ConditionStatus;
import dukku.common.shared.product.type.ProductEventType;
import dukku.common.shared.product.type.SaleStatus;
import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.entity.tag.ProductTag;
import dukku.product.boundedContext.product.entity.tag.Tag;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(
                        name = "idx_products_cat_vis_sale_created",
                        columnList = "category_id, visibility_status, sale_status, created_at"
                ),
                @Index(
                        name = "idx_products_cat_vis_sale_like",
                        columnList = "category_id, visibility_status, sale_status, like_count"
                ),
                @Index(
                        name = "idx_products_seller_deleted_created",
                        columnList = "seller_uuid, deleted_at, created_at"
                ),
                @Index(
                        name = "idx_products_seller_sale_deleted_created",
                        columnList = "seller_uuid, sale_status, deleted_at, created_at"
                )
        }
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseIdAndUUIDAndTime {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, columnDefinition = "uuid", comment = "판매자 UUID")
    private UUID sellerUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, comment = "카테고리")
    private Category category;

    @Column(nullable = false, length = 200, comment = "상품 제목")
    private String title;

    @Column(columnDefinition = "TEXT", comment = "상품 설명")
    private String description;

    @Column(nullable = false, comment = "상품 가격")
    private Long price;

    @Column(nullable = false, comment = "배송비")
    private Long shippingFee;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            comment = "상품 컨디션"
    )
    private ConditionStatus conditionStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            comment = "상품 판매 상태"
    )
    private SaleStatus saleStatus;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            comment = "상품 노출 상태"
    )
    private VisibilityStatus visibilityStatus;

    @Column(nullable = false, comment = "조회수")
    private int viewCount;

    @Column(nullable = false, comment = "좋아요 수")
    private int likeCount;

    @Column(nullable = false, comment = "댓글 수")
    private int commentCount;

    @Column(comment = "삭제일(소프트 삭제)")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid", comment = "현재 예약 중인 주문 UUID")
    private UUID reservedOrderUuid;

    public static Product create(
            UUID sellerUuid,
            Category category,
            String title,
            String description,
            Long price,
            Long shippingFee,
            ConditionStatus conditionStatus
    ) {
        return Product.builder()
                .sellerUuid(sellerUuid)
                .category(category)
                .title(title)
                .description(description)
                .price(price)
                .shippingFee(shippingFee == null ? 0L : shippingFee)

                .conditionStatus(conditionStatus == null ? ConditionStatus.SEALED : conditionStatus)
                .saleStatus(SaleStatus.ON_SALE)
                .visibilityStatus(VisibilityStatus.VISIBLE)

                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .build();
    }

    // 동일 주문 재요청은 허용하고, 타 주문의 예약 요청은 충돌 예외로 차단한다.
    public void reserve(UUID orderUuid) {
        if (this.saleStatus == SaleStatus.ON_SALE) {
            this.saleStatus = SaleStatus.RESERVED;
            this.reservedOrderUuid = orderUuid;
            return;
        }

        if (this.saleStatus == SaleStatus.RESERVED) {
            if (this.reservedOrderUuid != null && this.reservedOrderUuid.equals(orderUuid)) {
                // 동일 주문의 재요청은 멱등하게 통과
                return;
            }
            throw new ProductReservationConflictException("이미 다른 주문에서 거래 중인 상품입니다.");
        }

        throw new ProductReservationConflictException("판매 완료된 상품은 예약할 수 없습니다.");
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(String imageUrl) {
        int nextSortOrder = images.stream()
                .mapToInt(ProductImage::getSortOrder)
                .max()
                .orElse(0) + 1;

        images.add(ProductImage.create(this, imageUrl, nextSortOrder));
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductTag> productTags = new ArrayList<>();

    // 태그 교체: 동일 태그 재삽입으로 인한 unique 충돌을 막기 위해 차집합만 반영
    public void replaceTags(List<Tag> tags) {
        if (tags == null) {
            this.productTags.clear();
            return;
        }

        List<Tag> normalizedTags = tags.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Product::tagKey,
                                tag -> tag,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));

        Set<String> desiredKeys = normalizedTags.stream()
                .map(Product::tagKey)
                .collect(Collectors.toSet());

        this.productTags.removeIf(productTag -> !desiredKeys.contains(tagKey(productTag.getTag())));

        Set<String> existingKeys = this.productTags.stream()
                .map(productTag -> tagKey(productTag.getTag()))
                .collect(Collectors.toSet());

        for (Tag tag : normalizedTags) {
            String key = tagKey(tag);
            if (!existingKeys.contains(key)) {
                this.productTags.add(ProductTag.create(this, tag));
                existingKeys.add(key);
            }
        }
    }

    private static String tagKey(Tag tag) {
        if (tag.getId() != null) {
            return "id:" + tag.getId();
        }
        return "name:" + tag.getName();
    }

    // 읽기 전용으로 태그 이름 목록 반환 (ES 동기화용)
    public List<String> getTagNames() {
        return this.productTags.stream()
                .map(pt -> pt.getTag().getName())
                .toList();
    }

    // 무작정 바꾸는게 아닌 null이아닌것만 바꾼다.
    public void update(
            Category category,
            String title,
            String description,
            Long price,
            Long shippingFee,
            ConditionStatus conditionStatus,
            VisibilityStatus visibilityStatus
    ) {
        if (category != null) this.category = category;
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null) this.description = description;
        if (price != null) this.price = price;
        if (shippingFee != null) this.shippingFee = shippingFee;
        if (conditionStatus != null) this.conditionStatus = conditionStatus;
        if (visibilityStatus != null) this.visibilityStatus = visibilityStatus;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.visibilityStatus = VisibilityStatus.HIDDEN;
    }

    public void suspend() {
        this.visibilityStatus = VisibilityStatus.BLOCKED;
    }

    public void replaceImages(List<String> newImageUrls) {
        this.images.clear();

        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            for (String url : newImageUrls) {
                this.addImage(url);
            }
        }
    }

    // 판매 확정 (RESERVED -> SOLD_OUT)
    public void confirmSale(UUID orderUuid) {
        // 내 주문이 맞는지 검증 (다른 사람의 주문으로 예약된 상품을 건드리지 않도록)
        if (this.saleStatus == SaleStatus.RESERVED &&
                this.reservedOrderUuid != null &&
                this.reservedOrderUuid.equals(orderUuid)) {

            this.saleStatus = SaleStatus.SOLD_OUT;
            // 판매 완료되어도 주문 추적을 위해 reservedOrderUuid는 남겨두거나,
            // 별도 soldOrderUuid로 옮기는 정책을 쓸 수 있음. 여기선 유지.
        }
    }

    // 예약 해제 (RESERVED/SOLD_OUT -> ON_SALE)
    public void releaseReservation(UUID orderUuid) {
        // 내 주문이 맞는지 검증
        if ((this.saleStatus == SaleStatus.RESERVED || this.saleStatus == SaleStatus.SOLD_OUT) &&
                this.reservedOrderUuid != null &&
                this.reservedOrderUuid.equals(orderUuid)) {

            this.saleStatus = SaleStatus.ON_SALE;
            this.reservedOrderUuid = null; // 예약 정보 삭제
        }
    }

    public static ProductListItemResponse from(Product product) {
        String thumbnail = product.getImages() == null
                ? null
                : product.getImages().stream()
                .min(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductListItemResponse.builder()
                .productUuid(product.getUuid())
                .title(product.getTitle())
                .price(product.getPrice())
                .thumbnailUrl(thumbnail)
                .saleStatus(product.getSaleStatus())
                .likeCount(product.getLikeCount())
                .commentCount(product.getCommentCount())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .tagNames(product.getTagNames())
                .build();
    }

    public static ProductListResponse from(Page<Product> result) {
        return ProductListResponse.builder()
                .items(result.getContent().stream()
                        .map(Product::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalCount(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    public static ProductPayload toProductPayload(Product product, ProductEventType eventType) {
        return new ProductPayload(
                eventType,
                product.getUuid(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getShippingFee(),
                product.getCategory().getId(),
                product.getCategory().getCategoryName(),
                product.getTagNames(),
                product.getSaleStatus(),
                ProductMapper.buildProductUrl(product.getUuid())
        );
    }
}
