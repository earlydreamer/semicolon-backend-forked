package dukku.semicolon.boundedContext.coupon.in;

import dukku.common.global.UserUtil;
import dukku.semicolon.boundedContext.coupon.app.command.CouponFacade;
import dukku.semicolon.boundedContext.coupon.app.query.CouponQueryFacade;
import dukku.semicolon.shared.coupon.dto.CouponCreateRequest;
import dukku.semicolon.shared.coupon.dto.CouponResponse;
import dukku.semicolon.shared.coupon.dto.CouponUpdateRequest;
import jakarta.validation.Valid;
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

    // 쿠폰 생성 (관리자)
    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(@RequestBody @Valid CouponCreateRequest request) {
        return couponFacade.createCoupon(request);
    }

    // 쿠폰 초안 수정 (관리자)
    @PutMapping("/admin/{couponUuid}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDraft(
            @PathVariable UUID couponUuid,
            @RequestBody @Valid CouponUpdateRequest request
    ) {
        couponFacade.updateDraft(couponUuid, request);
    }

    // 쿠폰 활성화 (관리자)
    @PostMapping("/admin/{couponUuid}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID couponUuid) {
        couponFacade.activate(couponUuid);
    }

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
    public List<CouponResponse> getIssuableCoupons() {
        return couponQueryFacade.getIssuableCoupons(UserUtil.getUserId());
    }

    // 내가 보유한 쿠폰 리스트 (유저)
    @GetMapping("/me")
    public List<CouponResponse> getMyCoupons() {
        return couponQueryFacade.getMyCoupons(UserUtil.getUserId());
    }

    // 전체 쿠폰 리스트 (관리자)
    @GetMapping("/admin")
    public List<CouponResponse> getAllCoupons() {
        return couponQueryFacade.getAllCoupons();
    }
}
