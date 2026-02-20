package dukku.coupon.boundedContext.coupon.app.command;

import dukku.coupon.boundedContext.coupon.entity.Coupon;
import dukku.coupon.boundedContext.coupon.out.CouponRepository;
import dukku.coupon.boundedContext.coupon.out.CouponUserRepository;
import dukku.common.shared.coupon.type.CouponStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponSyncScheduler {

    private final CouponRepository couponRepository;
    private final CouponUserRepository couponUserRepository;

    // 1초마다 실행하여 DB 상태를 최신화 (부하에 따라 조절 가능)
    @Scheduled(fixedDelay = 60000 * 10)
    @Transactional
    public void syncCouponQuantities() {
        // 1. 현재 진행 중인(ACTIVE) 쿠폰만 조회
        List<Coupon> activeCoupons = couponRepository.findAllByStatus(CouponStatus.ACTIVE);

        for (Coupon coupon : activeCoupons) {
            // 2. [검증] 실제 발급된 내역(CouponUser)을 직접 카운트 (Source of Truth)
            long realIssuedCount = couponUserRepository.countByCouponId(coupon.getId());

            // 3. Coupon 엔티티의 수량과 실제 내역이 다르면 동기화
            if (coupon.getIssuedQuantity() != realIssuedCount) {
                log.info("쿠폰 수량 동기화 [UUID: {}] 기존: {} -> 변경: {}",
                        coupon.getUuid(), coupon.getIssuedQuantity(), realIssuedCount);

                coupon.syncIssuedQuantity((int) realIssuedCount);
            }

            // 4. [방어 로직] 만약 Redis 이슈로 초과 발급이 발생 할 경우
            if (realIssuedCount > coupon.getTotalQuantity()) {
                log.error("[CRITICAL] 초과 발급 발생! 쿠폰: {}, 총량: {}, 실제발급: {}",
                        coupon.getUuid(), coupon.getTotalQuantity(), realIssuedCount);

                // 즉시 해당 쿠폰을 마감 처리하여 추가 발급 차단
                coupon.expire();

                // TODO: 초과 발급된 사용자에게 안내 메시지 발송 or 취소 로직 트리거 (별도 처리)
            }

            // 5. 정상적으로 소진되었으면 마감 처리
            else if (realIssuedCount == coupon.getTotalQuantity()) {
                log.info("쿠폰 매진 처리 [UUID: {}]", coupon.getUuid());
                coupon.expire(); // 상태를 EXPIRED 또는 SOLD_OUT으로 변경
            }
        }
    }
}
