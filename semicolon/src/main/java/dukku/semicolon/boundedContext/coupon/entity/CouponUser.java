package dukku.semicolon.boundedContext.coupon.entity;

import dukku.common.global.exception.ConflictException;
import dukku.semicolon.boundedContext.coupon.entity.type.CouponUserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "coupon_users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_uuid", "coupon_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, updatable = false)
    private UUID userUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponUserStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    /* 발급 */
    public static CouponUser issue(UUID userUuid, Coupon coupon) {
        coupon.issue(); // 쿠폰 수량 차감

        CouponUser cu = new CouponUser();
        cu.userUuid = userUuid;
        cu.coupon = coupon;
        cu.status = CouponUserStatus.AVAILABLE;
        cu.issuedAt = LocalDateTime.now();
        return cu;
    }

    /* 사용 */
    public void use() {
        if (status != CouponUserStatus.AVAILABLE) {
            throw new ConflictException("사용할 수 없는 쿠폰");
        }
        this.status = CouponUserStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    /* 만료 */
    public void expire() {
        if (status == CouponUserStatus.USED) return;
        this.status = CouponUserStatus.EXPIRED;
    }
}
