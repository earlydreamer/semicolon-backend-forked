package dukku.coupon.global;

import dukku.common.shared.coupon.type.CouponStatus;
import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(4)
public class CouponInitData {

    @Bean
    public CommandLineRunner initCoupons(CouponRepository couponRepository) {
        return new CommandLineRunner() {
            @Override
            @Transactional
            public void run(String... args) {
                UUID couponUuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

                if (couponRepository.findByUuid(couponUuid).isPresent()) {
                    log.info("테스트 쿠폰이 이미 존재하여 초기화를 건너뜁니다.");
                    return;
                }

                Coupon coupon = Coupon.builder()
                        .uuid(couponUuid)
                        .couponName("선착순 배송비 할인 쿠폰")
                        .discountAmount(3000)
                        .minimumOrderAmount(0)
                        .validFrom(LocalDateTime.now().minusMinutes(1))
                        .status(CouponStatus.ACTIVE)
                        .totalQuantity(100)
                        .build();

                couponRepository.save(coupon);
                log.info("✅ 테스트용 선착순 쿠폰 생성 완료");
            }
        };
    }
}