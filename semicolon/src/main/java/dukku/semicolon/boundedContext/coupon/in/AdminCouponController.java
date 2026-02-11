package dukku.semicolon.boundedContext.coupon.in;

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
@RequestMapping("/api/v1/admin")
public class AdminCouponController {
    private final CouponQueryFacade couponQueryFacade;
    private final CouponFacade couponFacade;

    // 쿠폰 생성 (관리자)
    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse createCoupon(@RequestBody @Valid CouponCreateRequest request) {
        return couponFacade.createCoupon(request);
    }

    // 쿠폰 초안 수정 (관리자)
    @PutMapping("/coupons/{couponUuid}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDraft(
            @PathVariable UUID couponUuid,
            @RequestBody @Valid CouponUpdateRequest request
    ) {
        couponFacade.updateDraft(couponUuid, request);
    }

    // 쿠폰 활성화 (관리자)
    @PostMapping("/coupons/{couponUuid}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateCoupon(@PathVariable UUID couponUuid) {
        couponFacade.activateCoupon(couponUuid);
    }

    // 전체 쿠폰 리스트 (관리자)
    @GetMapping("/coupons")
    public List<CouponResponse> findAllCoupons() {
        return couponQueryFacade.findAllCoupons();
    }
}
