package dukku.user.boundedContext.user.entity;

import dukku.common.global.jpa.entity.BaseIdAndTime;
import dukku.user.boundedContext.user.type.UserSanctionAuditAction;
import dukku.user.boundedContext.user.type.UserSanctionReasonCode;
import dukku.user.boundedContext.user.type.UserSanctionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "user_sanction_audit_logs")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSanctionAuditLog extends BaseIdAndTime {

    @Column(name = "sanction_id", nullable = false)
    private Integer sanctionId;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_uuid", columnDefinition = "uuid", nullable = false)
    private UUID userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private UserSanctionAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 30)
    private UserSanctionType sanctionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private UserSanctionReasonCode reasonCode;

    @Column(name = "memo", length = 1000)
    private String memo;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "performed_by", columnDefinition = "uuid", nullable = false)
    private UUID performedBy;

    public static UserSanctionAuditLog create(
            UserSanction sanction,
            UserSanctionAuditAction action,
            UUID performedBy,
            String memo
    ) {
        return UserSanctionAuditLog.builder()
                .sanctionId(sanction.getId())
                .userUuid(sanction.getUser().getUuid())
                .action(action)
                .sanctionType(sanction.getSanctionType())
                .reasonCode(sanction.getReasonCode())
                .memo(memo)
                .performedBy(performedBy)
                .build();
    }
}
