package dukku.common.shared.user.type;

import lombok.Getter;

@Getter
public enum Role {
    USER("회원"),
    ADMIN("관리자"),
    SYSTEM("시스템");

    private final String label;

    Role(String label) {
        this.label = label;
    }
}
