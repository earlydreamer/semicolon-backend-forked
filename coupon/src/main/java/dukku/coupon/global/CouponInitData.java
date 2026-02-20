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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(1)
public class CouponInitData {
    private final RedisTemplate<String, String> redisTemplate;

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

                String redisKey = "coupon:count:" + couponUuid;

                // DB에서 실제 수량 조회 (서버 뜰 때 딱 한 번 실행됨)
                int totalQuantity = 100; // 또는 couponRepository에서 조회 가능

                // Redis에 수량 초기화
                redisTemplate.opsForValue().set(redisKey, String.valueOf(totalQuantity));
                System.out.println("✅ Redis Warm-up 완료: " + redisKey + " = " + totalQuantity);

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