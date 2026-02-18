package dukku.user.boundedContext.user.type;

import lombok.Getter;

@Getter
public enum UserSanctionStatus {
    ACTIVE("적용 중"),
    EXPIRED("만료"),
    REVOKED("해제");

    private final String label;

    UserSanctionStatus(String label) {
        this.label = label;
    }
}
