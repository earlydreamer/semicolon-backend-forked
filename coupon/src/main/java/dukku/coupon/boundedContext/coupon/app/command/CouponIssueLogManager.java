package dukku.coupon.boundedContext.coupon.app.command;

import dukku.coupon.boundedContext.coupon.entity.CouponIssueLog;
import dukku.coupon.boundedContext.coupon.entity.type.IssueResult;
import dukku.coupon.boundedContext.coupon.out.CouponIssueLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponIssueLogManager {
    private final CouponIssueLogRepository couponIssueLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID couponUuid, UUID userUuid, IssueResult result, LocalDateTime requestedAt) {
        couponIssueLogRepository.save(
                CouponIssueLog.of(couponUuid, userUuid, result, requestedAt)
        );
    }
}
