package dukku.coupon.boundedContext.coupon.app.command;

import dukku.common.shared.coupon.dto.CouponCreateRequest;
import dukku.common.shared.coupon.dto.CouponResponse;
import dukku.common.shared.coupon.type.CouponStatus;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCouponUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CreateCouponUseCase useCase;

    @Test
    @DisplayName("쿠폰 생성 요청을 저장하고 CouponResponse를 반환한다")
    void executeSavesCouponAndReturnsResponse() {
        LocalDateTime validFrom = LocalDateTime.now().plusDays(1);
        CouponCreateRequest request = new CouponCreateRequest(
                "오픈 기념 쿠폰",
                2000,
                10000,
                validFrom,
                30
        );

        UUID couponUuid = UUID.randomUUID();
        when(couponRepository.save(org.mockito.ArgumentMatchers.any(Coupon.class)))
                .thenAnswer(invocation -> {
                    Coupon saved = invocation.getArgument(0);
                    return Coupon.builder()
                            .id(1)
                            .uuid(couponUuid)
                            .couponName(saved.getCouponName())
                            .discountAmount(saved.getDiscountAmount())
                            .minimumOrderAmount(saved.getMinimumOrderAmount())
                            .validFrom(saved.getValidFrom())
                            .createdAt(LocalDateTime.now())
                            .status(saved.getStatus())
                            .totalQuantity(saved.getTotalQuantity())
                            .issuedQuantity(saved.getIssuedQuantity())
                            .build();
                });

        CouponResponse response = useCase.execute(request);

        ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
        verify(couponRepository).save(captor.capture());
        Coupon persisted = captor.getValue();
        assertThat(persisted.getCouponName()).isEqualTo("오픈 기념 쿠폰");
        assertThat(persisted.getDiscountAmount()).isEqualTo(2000);
        assertThat(persisted.getMinimumOrderAmount()).isEqualTo(10000);
        assertThat(persisted.getValidFrom()).isEqualTo(validFrom);
        assertThat(persisted.getStatus()).isEqualTo(CouponStatus.DRAFT);
        assertThat(persisted.getIssuedQuantity()).isZero();
        assertThat(persisted.getTotalQuantity()).isEqualTo(30);

        assertThat(response.uuid()).isEqualTo(couponUuid);
        assertThat(response.couponName()).isEqualTo("오픈 기념 쿠폰");
        assertThat(response.status()).isEqualTo(CouponStatus.DRAFT);
        assertThat(response.issuedQuantity()).isZero();
        assertThat(response.totalQuantity()).isEqualTo(30);
    }
}

