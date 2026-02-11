package dukku.coupon.boundedContext.coupon.out;

import dukku.coupon.boundedContext.coupon.entity.CouponIssueLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueLogRepository extends JpaRepository<CouponIssueLog, Integer> {
}
