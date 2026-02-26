package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.dto.CouponResponse;
import dukku.common.shared.coupon.dto.CouponUpdateRequest;
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
    private final DeactivateCouponUseCase deactivateCouponUseCase;
    private final DeleteCouponUseCase deleteCouponUseCase;

    // 쿠폰을 생성한다
    public CouponResponse createCoupon(CouponCreateRequest request) {
        return createCouponUseCase.execute(request);
    }

    // 초안 상태의 쿠폰 정보를 수정한다
    public void updateDraft(UUID couponUuid, CouponUpdateRequest request) {
        updateCouponUseCase.execute(couponUuid, request);
    }

    // 쿠폰을 활성화 상태로 변경한다
    public void activateCoupon(UUID couponUuid) {
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

    // 결제 실패 시 사용했던 쿠폰을 복구한다.
    public void rollbackCouponUseForPayment(UUID userUuid, UUID couponUuid) {
        useCouponUseCase.rollbackForPayment(userUuid, couponUuid);
    }

    public void deactivateCoupon(UUID couponUuid) {
        deactivateCouponUseCase.execute(couponUuid);
    }

    public void deleteCoupon(UUID couponUuid) {
        deleteCouponUseCase.execute(couponUuid);
    }

}
