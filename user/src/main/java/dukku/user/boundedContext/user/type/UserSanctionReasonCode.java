package dukku.user.boundedContext.user.type;

import lombok.Getter;

@Getter
public enum UserSanctionReasonCode {
    FRAUD_TRANSACTION("사기/결제 악용"),
    FALSE_LISTING("허위 매물/가품"),
    SPAM_ADVERTISEMENT("스팸/광고"),
    ABUSE_HARASSMENT("욕설/괴롭힘"),
    MULTI_ACCOUNT_EVASION("다중 계정/정지 우회"),
    ILLEGAL_GOODS("금지 품목/불법 콘텐츠"),
    SECURITY_THREAT("보안 위협");

    private final String label;

    UserSanctionReasonCode(String label) {
        this.label = label;
    }
}
