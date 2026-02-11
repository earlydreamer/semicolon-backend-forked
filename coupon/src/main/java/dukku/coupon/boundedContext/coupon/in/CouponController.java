package dukku.coupon.boundedContext.coupon.in;

import dukku.common.global.UserUtil;
import dukku.coupon.boundedContext.coupon.app.command.CouponFacade;
import dukku.coupon.boundedContext.coupon.app.query.CouponQueryFacade;
import dukku.coupon.shared.coupon.dto.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {
    private final CouponQueryFacade couponQueryFacade;
    private final CouponFacade couponFacade;

    // 쿠폰 발급 (유저)
    @PostMapping("/{couponUuid}/issue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void issueCoupon(@PathVariable UUID couponUuid) {
        couponFacade.issueCoupon(UserUtil.getUserId(), couponUuid);
    }

    // 쿠폰 사용 (유저)
    @PostMapping("/{couponUuid}/use")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void useCoupon(@PathVariable UUID couponUuid) {
        couponFacade.useCoupon(UserUtil.getUserId(), couponUuid);
    }

    // 발급 가능한 쿠폰 리스트 (유저)
    @GetMapping("/issuable")
    public List<CouponResponse> findIssuableCoupons() {
        return couponQueryFacade.findIssuableCoupons(UserUtil.getUserId());
    }

    // 내가 보유한 쿠폰 리스트 (유저)
    @GetMapping("/me")
    public List<CouponResponse> findMyCoupons() {
        return couponQueryFacade.findMyCoupons(UserUtil.getUserId());
    }
}
