package dukku.user.boundedContext.user.entity;

import dukku.common.global.auth.crypto.converter.AesGcmConverter;
import dukku.common.shared.user.domain.SourceUser;
import dukku.common.shared.user.dto.UserDto;
import dukku.common.shared.user.dto.UserRegisterRequest;
import dukku.common.shared.user.dto.UserResponse;
import dukku.common.shared.user.dto.UserUpdateRequest;
import dukku.common.shared.user.type.Role;
import dukku.common.shared.user.type.UserStatus;
import dukku.common.shared.user.exception.UserAlreadyWithdrawException;
import dukku.common.shared.user.exception.UserWithdrawRestoreNotAllowedException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SourceUser {
    @Column(length = 100, nullable = false, comment = "암호화된 비밀번호")
    private String password;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "withdrawal_email_backup", length = 255)
    private String withdrawalEmailBackup;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "withdrawal_nickname_backup", length = 100)
    private String withdrawalNicknameBackup;


    public static User createUser(UserRegisterRequest req, Role role, String encodedPassword) {
        return User.builder()
                .email(req.getEmail())
                .password(encodedPassword)
                .role(role)
                .nickname(req.getNickname())
                .build();
    }

    public void updateUser(UserUpdateRequest req) {
        this.setNickname(req.getName());
    }

    public void updateRole(Role role) {
        this.setRole(role);
    }

    public void updateStatus(UserStatus status) {
        this.setStatus(status);
    }

    public void withdraw(String maskedEmail, String maskedNickname, String encodedPassword) {
        if (isWithdrawnStatus()) {
            throw new UserAlreadyWithdrawException();
        }
        this.withdrawalEmailBackup = this.getEmail();
        this.withdrawalNicknameBackup = this.getNickname();
        this.setEmail(maskedEmail);
        this.setNickname(maskedNickname);
        this.password = encodedPassword;
        this.setStatus(UserStatus.WITHDRAWN_PENDING);
        this.setDeletedAt(LocalDateTime.now());
    }

    public void restoreFromWithdrawal(String encodedPassword) {
        if (this.getStatus() != UserStatus.WITHDRAWN_PENDING && this.getStatus() != UserStatus.DELETED) {
            throw UserWithdrawRestoreNotAllowedException.userIsNotRestorable();
        }
        if (this.withdrawalEmailBackup == null) {
            throw UserWithdrawRestoreNotAllowedException.missingWithdrawalBackup();
        }
        this.setEmail(this.withdrawalEmailBackup);
        this.setNickname(this.withdrawalNicknameBackup);
        this.password = encodedPassword;
        this.withdrawalEmailBackup = null;
        this.withdrawalNicknameBackup = null;
        this.setStatus(UserStatus.ACTIVE);
        this.setDeletedAt(null);
    }

    public void finalizeWithdrawal(String encodedPassword) {
        if (!isWithdrawnStatus()) {
            return;
        }
        this.password = encodedPassword;
        this.withdrawalEmailBackup = null;
        this.withdrawalNicknameBackup = null;
        this.setStatus(UserStatus.WITHDRAWN_FINAL);
        if (this.getDeletedAt() == null) {
            this.setDeletedAt(LocalDateTime.now());
        }
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUuid(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    public static UserDto toUserDto(User user) {
        return new UserDto(
                user.getUuid(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus()
        );
    }

    private boolean isWithdrawnStatus() {
        return this.getStatus() == UserStatus.WITHDRAWN_PENDING
                || this.getStatus() == UserStatus.WITHDRAWN_FINAL
                || this.getStatus() == UserStatus.DELETED;
    }
}