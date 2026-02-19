package dukku.user.boundedContext.user.in.dto;

import dukku.user.boundedContext.user.entity.UserSanction;
import dukku.user.boundedContext.user.type.UserSanctionReasonCode;
import dukku.user.boundedContext.user.type.UserSanctionStatus;
import dukku.user.boundedContext.user.type.UserSanctionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AdminUserSanctionResponse {
    private Integer sanctionId;
    private UserSanctionType sanctionType;
    private String sanctionTypeLabel;
    private UserSanctionReasonCode reasonCode;
    private String reasonCodeLabel;
    private UserSanctionStatus status;
    private String statusLabel;
    private String memo;
    private String evidenceUrl;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private UUID createdBy;
    private UUID revokedBy;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserSanctionResponse from(UserSanction sanction) {
        return AdminUserSanctionResponse.builder()
                .sanctionId(sanction.getId())
                .sanctionType(sanction.getSanctionType())
                .sanctionTypeLabel(sanction.getSanctionType().getLabel())
                .reasonCode(sanction.getReasonCode())
                .reasonCodeLabel(sanction.getReasonCode().getLabel())
                .status(sanction.getStatus())
                .statusLabel(sanction.getStatus().getLabel())
                .memo(sanction.getMemo())
                .evidenceUrl(sanction.getEvidenceUrl())
                .startAt(sanction.getStartAt())
                .endAt(sanction.getEndAt())
                .createdBy(sanction.getCreatedBy())
                .revokedBy(sanction.getRevokedBy())
                .revokedAt(sanction.getRevokedAt())
                .createdAt(sanction.getCreatedAt())
                .updatedAt(sanction.getUpdatedAt())
                .build();
    }
}
