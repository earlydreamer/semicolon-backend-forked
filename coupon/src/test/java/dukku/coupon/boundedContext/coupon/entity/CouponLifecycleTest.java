package dukku.coupon.boundedContext.coupon.entity;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.dto.CouponUpdateRequest;
import dukku.common.shared.coupon.exception.CouponActivationNotAllowedException;
import dukku.common.shared.coupon.exception.CouponDeactivationNotAllowedException;
import dukku.common.shared.coupon.exception.CouponIssueNotAllowedException;
import dukku.common.shared.coupon.exception.CouponSoldOutException;
import dukku.common.shared.coupon.exception.CouponUpdateNotAllowedException;
import dukku.common.shared.coupon.type.CouponStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponLifecycleTest {

    @Test
    @DisplayName("createCoupon은 DRAFT 상태와 issuedQuantity 0으로 초기화한다")
    void createCouponInitializesDraftAndIssuedZero() {
        CouponCreateRequest request = new CouponCreateRequest(
                "신규 쿠폰",
                3000,
                10000,
                LocalDateTime.now().plusDays(1),
                50
        );

        Coupon coupon = Coupon.createCoupon(request);

        assertThat(coupon.getCouponName()).isEqualTo("신규 쿠폰");
        assertThat(coupon.getDiscountAmount()).isEqualTo(3000);
        assertThat(coupon.getMinimumOrderAmount()).isEqualTo(10000);
        assertThat(coupon.getValidFrom()).isEqualTo(request.validFrom());
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.DRAFT);
        assertThat(coupon.getIssuedQuantity()).isEqualTo(0);
        assertThat(coupon.getTotalQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("DRAFT 상태에서 updateDraft가 필드를 갱신한다")
    void updateDraftUpdatesFields() {
        Coupon coupon = draftCoupon();
        CouponUpdateRequest request = new CouponUpdateRequest(
                "수정 쿠폰",
                5000,
                15000,
                LocalDateTime.now().plusDays(2)
        );

        coupon.updateDraft(request);

        assertThat(coupon.getCouponName()).isEqualTo("수정 쿠폰");
        assertThat(coupon.getDiscountAmount()).isEqualTo(5000);
        assertThat(coupon.getMinimumOrderAmount()).isEqualTo(15000);
        assertThat(coupon.getValidFrom()).isEqualTo(request.validFrom());
    }

    @Test
    @DisplayName("DRAFT가 아닌 상태에서 updateDraft를 호출하면 예외가 발생한다")
    void updateDraftThrowsWhenNotDraft() {
        Coupon coupon = activeCoupon(10, 0);

        assertThatThrownBy(() -> coupon.updateDraft(
                new CouponUpdateRequest("x", 1000, 0, LocalDateTime.now())
        )).isInstanceOf(CouponUpdateNotAllowedException.class);
    }

    @Test
    @DisplayName("activate/deactivate 상태 전이가 정상 동작한다")
    void activateAndDeactivateWork() {
        Coupon coupon = draftCoupon();

        coupon.activate();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);

        coupon.deactivate();
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.INACTIVE);
    }

    @Test
    @DisplayName("잘못된 activate/deactivate 호출 시 예외가 발생한다")
    void activateDeactivateThrowWhenInvalid() {
        Coupon active = activeCoupon(10, 0);
        Coupon draft = draftCoupon();

        assertThatThrownBy(active::activate).isInstanceOf(CouponActivationNotAllowedException.class);
        assertThatThrownBy(draft::deactivate).isInstanceOf(CouponDeactivationNotAllowedException.class);
    }

    @Test
    @DisplayName("ACTIVE 상태에서 issue는 발급 수량을 증가시킨다")
    void issueIncrementsIssuedQuantity() {
        Coupon coupon = activeCoupon(3, 1);

        coupon.issue();

        assertThat(coupon.getIssuedQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("ACTIVE가 아니면 issue는 예외가 발생한다")
    void issueThrowsWhenNotActive() {
        Coupon coupon = draftCoupon();

        assertThatThrownBy(coupon::issue).isInstanceOf(CouponIssueNotAllowedException.class);
    }

    @Test
    @DisplayName("발급 수량이 총 수량에 도달하면 issue는 품절 예외가 발생한다")
    void issueThrowsWhenSoldOut() {
        Coupon coupon = activeCoupon(2, 2);

        assertThatThrownBy(coupon::issue).isInstanceOf(CouponSoldOutException.class);
    }

    @Test
    @DisplayName("syncIssuedQuantity는 실제 발급량으로 덮어쓴다")
    void syncIssuedQuantitySetsRealValue() {
        Coupon coupon = activeCoupon(100, 10);

        coupon.syncIssuedQuantity(42);

        assertThat(coupon.getIssuedQuantity()).isEqualTo(42);
    }

    private Coupon draftCoupon() {
        return Coupon.builder()
                .uuid(UUID.randomUUID())
                .couponName("초안 쿠폰")
                .discountAmount(1000)
                .minimumOrderAmount(5000)
                .validFrom(LocalDateTime.now().plusDays(1))
                .status(CouponStatus.DRAFT)
                .totalQuantity(100)
                .issuedQuantity(0)
                .build();
    }

    private Coupon activeCoupon(int totalQuantity, int issuedQuantity) {
        return Coupon.builder()
                .uuid(UUID.randomUUID())
                .couponName("활성 쿠폰")
                .discountAmount(2000)
                .minimumOrderAmount(7000)
                .validFrom(LocalDateTime.now().minusDays(1))
                .status(CouponStatus.ACTIVE)
                .totalQuantity(totalQuantity)
                .issuedQuantity(issuedQuantity)
                .build();
    }
}

