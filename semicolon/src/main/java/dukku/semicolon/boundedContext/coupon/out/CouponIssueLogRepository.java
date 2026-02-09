package dukku.semicolon.boundedContext.coupon.out;

import dukku.semicolon.boundedContext.coupon.entity.CouponIssueLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueLogRepository extends JpaRepository<CouponIssueLog, Integer> {
}
