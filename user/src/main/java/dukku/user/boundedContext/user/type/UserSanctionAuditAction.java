package dukku.user.boundedContext.user.type;

import lombok.Getter;

@Getter
public enum UserSanctionAuditAction {
    APPLIED("등록"),
    REVOKED("해제"),
    EXPIRED("만료");

    private final String label;

    UserSanctionAuditAction(String label) {
        this.label = label;
    }
}
