package dukku.common.shared.user.type;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("활성"),
    SUSPENDED("일시정지"),
    BANNED("영구정지"),
    WITHDRAWN_PENDING("탈퇴 대기"),
    WITHDRAWN_FINAL("탈퇴 완료"),
    DELETED("삭제됨"),
    BLOCKED("차단(레거시)");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }
}
