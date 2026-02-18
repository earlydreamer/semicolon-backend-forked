package dukku.user.boundedContext.user.entity;

import dukku.common.global.jpa.entity.BaseIdAndTime;
import dukku.user.boundedContext.user.type.UserSanctionReasonCode;
import dukku.user.boundedContext.user.type.UserSanctionStatus;
import dukku.user.boundedContext.user.type.UserSanctionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sanctions")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSanction extends BaseIdAndTime {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 30)
    private UserSanctionType sanctionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private UserSanctionReasonCode reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserSanctionStatus status;

    @Column(length = 1000, nullable = false)
    private String memo;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", columnDefinition = "uuid", nullable = false)
    private UUID createdBy;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "revoked_by", columnDefinition = "uuid")
    private UUID revokedBy;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static UserSanction apply(
            User user,
            UserSanctionType sanctionType,
            UserSanctionReasonCode reasonCode,
            String memo,
            String evidenceUrl,
            LocalDateTime startAt,
            LocalDateTime endAt,
            UUID createdBy
    ) {
        return UserSanction.builder()
                .user(user)
                .sanctionType(sanctionType)
                .reasonCode(reasonCode)
                .status(UserSanctionStatus.ACTIVE)
                .memo(memo)
                .evidenceUrl(evidenceUrl)
                .startAt(startAt)
                .endAt(endAt)
                .createdBy(createdBy)
                .build();
    }

    public boolean isEffectiveAt(LocalDateTime at) {
        if (status != UserSanctionStatus.ACTIVE) {
            return false;
        }
        if (startAt.isAfter(at)) {
            return false;
        }
        return endAt == null || endAt.isAfter(at);
    }

    public void revoke(UUID revokedBy, LocalDateTime revokedAt) {
        this.status = UserSanctionStatus.REVOKED;
        this.revokedBy = revokedBy;
        this.revokedAt = revokedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        this.status = UserSanctionStatus.EXPIRED;
        this.revokedAt = expiredAt;
    }
}
