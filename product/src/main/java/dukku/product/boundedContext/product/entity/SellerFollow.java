package dukku.product.boundedContext.product.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "seller_follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_seller_follow_user_seller",
                        columnNames = {"user_uuid", "seller_uuid"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerFollow extends BaseIdAndUUIDAndTime {

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name="user_uuid", nullable=false, comment="팔로워(요청자) UUID")
    private UUID userUuid;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "seller_uuid", nullable = false, comment = "팔로우 대상(판매자) UUID")
    private UUID sellerUuid;

    public static SellerFollow create(UUID userUuid, UUID sellerUuid) {
        return SellerFollow.builder()
                .userUuid(userUuid)
                .sellerUuid(sellerUuid)
                .build();
    }
}
