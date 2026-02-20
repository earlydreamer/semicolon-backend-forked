package dukku.coupon.boundedContext.coupon.in;

import dukku.common.shared.coupon.dto.CouponInternalResponse;
import dukku.coupon.boundedContext.coupon.app.query.CouponQueryFacade;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 쿠폰 Internal API 컨트롤러
 *
 * <p>
 * 결제 등 내부 서비스에서 쿠폰 정보를 조회하기 위한 API를 제공합니다.
 *
 * <p>
 * <b>보안:</b> 이 API는 내부 서비스 간 통신 전용이며 외부 노출하지 않습니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/coupons")
@RequiredArgsConstructor
@Hidden
public class CouponInternalController {

    private final CouponQueryFacade couponQueryFacade;

    /**
     * 쿠폰 단건 조회 (Internal API)
     *
     * <p>
     * 결제 서비스 등 내부 모듈에서 쿠폰 UUID로 쿠폰 상태 및 할인 정보 조회
     *
     * @param couponUuid 조회할 쿠폰 UUID
     * @return 쿠폰 내부 응답 DTO ({@link CouponInternalResponse})
     */
    @GetMapping("/{couponUuid}")
    public ResponseEntity<CouponInternalResponse> getCouponInfo(@PathVariable UUID couponUuid) {
        log.debug("쿠폰 내부 조회 요청. couponUuid={}", couponUuid);
        CouponInternalResponse response = couponQueryFacade.findCouponInfo(couponUuid);
        return ResponseEntity.ok(response);
    }
}
