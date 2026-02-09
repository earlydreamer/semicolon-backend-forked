package dukku.semicolon.boundedContext.coupon.app.query;

import dukku.semicolon.shared.coupon.dto.CouponResponse;
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
    public List<CouponResponse> getIssuableCoupons(UUID userUuid) {
        return couponQueryService.getIssuableCoupons(userUuid);
    }

    // 사용자가 보유한 쿠폰 리스트
    public List<CouponResponse> getMyCoupons(UUID userUuid) {
        return couponQueryService.getMyCoupons(userUuid);
    }

    // 관리자용 전체 쿠폰 리스트
    public List<CouponResponse> getAllCoupons() {
        return couponQueryService.getAllCoupons();
    }
}

