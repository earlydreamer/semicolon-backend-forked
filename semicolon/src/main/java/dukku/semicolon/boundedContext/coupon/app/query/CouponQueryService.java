package dukku.semicolon.boundedContext.coupon.app.query;

import dukku.semicolon.boundedContext.coupon.entity.CouponUser;
import dukku.semicolon.boundedContext.coupon.entity.type.CouponStatus;
import dukku.semicolon.boundedContext.coupon.entity.type.CouponUserStatus;
import dukku.semicolon.boundedContext.coupon.out.CouponRepository;
import dukku.semicolon.boundedContext.coupon.out.CouponUserRepository;
import dukku.semicolon.shared.coupon.dto.CouponResponse;
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
    public List<CouponResponse> getIssuableCoupons(UUID userUuid) {
        return couponRepository.findByStatus(CouponStatus.ACTIVE)
                .stream()
                .filter(coupon ->
                        !couponUserRepository.existsByUserUuidAndCoupon_Uuid(
                                userUuid,
                                coupon.getUuid()
                        )
                )
                .map(CouponResponse::from)
                .toList();
    }

    /**
     * 사용자가 보유한 쿠폰 목록
     */
    public List<CouponResponse> getMyCoupons(UUID userUuid) {
        List<CouponUser> couponUsers = couponUserRepository.findByUserUuidAndStatus(userUuid, CouponUserStatus.AVAILABLE);

        return couponUsers.stream()
                .map(CouponUser::getCoupon)
                .map(CouponResponse::from)
                .toList();
    }

    /**
     * 관리자용 전체 쿠폰 목록
     */
    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(CouponResponse::from)
                .toList();
    }
}
