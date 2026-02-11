package dukku.coupon.boundedContext.coupon.app.query;

import dukku.coupon.shared.coupon.dto.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryFacade {
    private final CouponQueryService couponQueryService;

    // 사용자에게 발급 가능한 쿠폰 리스트
    public List<CouponResponse> findIssuableCoupons(UUID userUuid) {
        return couponQueryService.findIssuableCoupons(userUuid);
    }

    // 사용자가 보유한 쿠폰 리스트
    public List<CouponResponse> findMyCoupons(UUID userUuid) {
        return couponQueryService.findMyCoupons(userUuid);
    }

    // 관리자용 전체 쿠폰 리스트
    public List<CouponResponse> findAllCoupons() {
        return couponQueryService.findAllCoupons();
    }
}

