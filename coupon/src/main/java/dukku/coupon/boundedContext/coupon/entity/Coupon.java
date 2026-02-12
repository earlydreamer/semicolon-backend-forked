package dukku.coupon.boundedContext.coupon.entity;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.dto.CouponResponse;
import dukku.common.shared.coupon.dto.CouponUpdateRequest;
import dukku.common.shared.coupon.exception.*;
import dukku.common.shared.coupon.type.CouponStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@EntityListeners(AuditingEntityListener.class)
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 100)
    private String couponName;

    @Column(nullable = false)
    private int discountAmount;

    @Column(nullable = false)
    private int minimumOrderAmount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponStatus status;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    // 쿠폰 생성 시 초기 상태는 DRAFT
    public static Coupon createCoupon(CouponCreateRequest request) {
        return Coupon.builder()
                .couponName(request.couponName())
                .discountAmount(request.discountAmount())
                .minimumOrderAmount(request.minimumOrderAmount())
                .validFrom(request.validFrom())
                .status(CouponStatus.DRAFT)
                .issuedQuantity(0)
                .totalQuantity(request.totalQuantity())
                .build();
    }

    // 발급 전(Active로 전환 전) 수정 허용
    public void updateDraft(CouponUpdateRequest request) {
        if (status != CouponStatus.DRAFT) {
            throw new CouponUpdateNotAllowedException();
        }
        this.couponName = request.couponName();
        this.discountAmount = request.discountAmount();
        this.minimumOrderAmount = request.minimumOrderAmount();
        this.validFrom = request.validFrom();
    }

    /* 상태 전이 */
    public void activate() {
        if (status != CouponStatus.DRAFT) {
            throw new CouponActivationNotAllowedException();
        }
        this.status = CouponStatus.ACTIVE;
    }

    public void deactivate() {
        if (status != CouponStatus.ACTIVE) {
            throw new CouponDeactivationNotAllowedException();
        }
        this.status = CouponStatus.INACTIVE;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }

    /* 발급 */
    public void issue() {
        if (status != CouponStatus.ACTIVE) {
            throw new CouponIssueNotAllowedException();
        }
        if (issuedQuantity >= totalQuantity) {
            throw new CouponSoldOutException();
        }
        this.issuedQuantity++;
    }

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getUuid(),
                coupon.getCouponName(),
                coupon.getDiscountAmount(),
                coupon.getMinimumOrderAmount(),
                coupon.getValidFrom(),
                coupon.getCreatedAt(),
                coupon.getStatus(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity()
        );
    }
}

