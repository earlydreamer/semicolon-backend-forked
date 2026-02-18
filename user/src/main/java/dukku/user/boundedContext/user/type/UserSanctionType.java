package dukku.user.boundedContext.user.type;

import lombok.Getter;

@Getter
public enum UserSanctionType {
    WARNING("경고"),
    SUSPENSION("일시정지"),
    PERMANENT_BAN("영구정지");

    private final String label;

    UserSanctionType(String label) {
        this.label = label;
    }
}
