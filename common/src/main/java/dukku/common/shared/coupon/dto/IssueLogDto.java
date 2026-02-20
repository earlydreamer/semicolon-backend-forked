package dukku.common.shared.coupon.dto;

import dukku.common.shared.coupon.type.IssueResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record IssueLogDto(
        UUID couponUuid,
        UUID userUuid,
        IssueResult result,
        LocalDateTime requestedAt
) {}