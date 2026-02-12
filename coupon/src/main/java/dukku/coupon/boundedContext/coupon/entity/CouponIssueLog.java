package dukku.coupon.boundedContext.coupon.entity;

import dukku.common.shared.coupon.type.IssueResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_issue_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private UUID couponUuid;

    @Column(nullable = false)
    private UUID userUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueResult result;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int elapsedMs;

    public static CouponIssueLog of(
            UUID couponUuid,
            UUID userUuid,
            IssueResult result,
            LocalDateTime requestedAt
    ) {
        CouponIssueLog log = new CouponIssueLog();
        log.couponUuid = couponUuid;
        log.userUuid = userUuid;
        log.result = result;
        log.requestedAt = requestedAt;
        log.processedAt = LocalDateTime.now();
        log.elapsedMs =
                (int) java.time.Duration.between(
                        requestedAt, log.processedAt
                ).toMillis();
        return log;
    }
}
