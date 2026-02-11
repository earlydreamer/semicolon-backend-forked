package dukku.product.boundedContext.product.entity.tag;

import dukku.product.boundedContext.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "product_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_tag", // 제약조건 이름
                        columnNames = {"product_id", "tag_id"} // 이 두 컬럼의 조합은 유일해야 함
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ProductTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static ProductTag create(Product product, Tag tag) {
        return ProductTag.builder()
                .product(product)
                .tag(tag)
                .build();
    }
}