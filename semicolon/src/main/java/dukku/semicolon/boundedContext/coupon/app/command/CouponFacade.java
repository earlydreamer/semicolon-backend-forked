package dukku.semicolon.boundedContext.coupon.app.command;

import dukku.semicolon.shared.coupon.dto.CouponCreateRequest;
import dukku.semicolon.shared.coupon.dto.CouponResponse;
import dukku.semicolon.shared.coupon.dto.CouponUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponFacade {
    private final CreateCouponUseCase createCouponUseCase;
    private final UpdateCouponUseCase updateCouponUseCase;
    private final ActivateCouponUseCase activateCouponUseCase;
    private final IssueCouponUseCase issueCouponUseCase;
    private final UseCouponUseCase useCouponUseCase;

    // 쿠폰을 생성한다
    public CouponResponse createCoupon(CouponCreateRequest request) {
        return createCouponUseCase.execute(request);
    }

    // 초안 상태의 쿠폰 정보를 수정한다
    public void updateDraft(UUID couponUuid, CouponUpdateRequest request) {
        updateCouponUseCase.execute(couponUuid, request);
    }

    // 쿠폰을 활성화 상태로 변경한다
    public void activate(UUID couponUuid) {
        activateCouponUseCase.execute(couponUuid);
    }

    // 사용자에게 쿠폰을 발급한다
    public void issueCoupon(UUID userUuid, UUID couponUuid) {
        issueCouponUseCase.execute(userUuid, couponUuid);
    }

    // 사용자가 쿠폰을 사용 처리한다
    public void useCoupon(UUID userUuid, UUID couponUuid) {
        useCouponUseCase.execute(userUuid, couponUuid);
    }

}
