package dukku.coupon.boundedContext.coupon.app.query;

import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.common.shared.coupon.dto.CouponResponse;
import dukku.common.shared.coupon.exception.CouponNotFoundException;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.common.shared.coupon.type.CouponUserStatus;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.entity.CouponUser;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryService {
    private final CouponRepository couponRepository;
    private final CouponUserRepository couponUserRepository;

    /**
     * 사용자가 발급 가능한 쿠폰 목록
     * - ACTIVE 상태
     * - 아직 해당 쿠폰을 발급받지 않은 경우
     */
    public List<CouponResponse> findIssuableCoupons(UUID userUuid) {
        return couponRepository.findByStatus(CouponStatus.ACTIVE)
                .stream()
                .filter(coupon ->
                        !couponUserRepository.existsByUserUuidAndCoupon_Uuid(
                                userUuid,
                                coupon.getUuid()
                        )
                )
                .map(Coupon::from)
                .toList();
    }

    /**
     * 사용자가 보유한 쿠폰 목록
     */
    public List<CouponResponse> findMyCoupons(UUID userUuid) {
        List<CouponUser> couponUsers = couponUserRepository.findByUserUuidAndStatus(userUuid, CouponUserStatus.AVAILABLE);

        return couponUsers.stream()
                .map(CouponUser::getCoupon)
                .map(Coupon::from)
                .toList();
    }

    /**
     * 관리자용 전체 쿠폰 목록
     */
    public List<CouponResponse> findAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(Coupon::from)
                .toList();
    }

    /**
     * 쿠폰 UUID로 단건 조회 (Internal API용)
     */
    public CouponInternalResponse findCouponInfo(UUID couponUuid) {
        Coupon coupon = couponRepository.findByUuid(couponUuid)
                .orElseThrow(CouponNotFoundException::new);

        return new CouponInternalResponse(
                coupon.getDiscountAmount(),
                coupon.getMinimumOrderAmount(),
                coupon.getStatus()
        );
    }
}
