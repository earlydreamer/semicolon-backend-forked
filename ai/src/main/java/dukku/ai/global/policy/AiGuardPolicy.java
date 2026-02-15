package dukku.ai.global.policy;

import java.util.List;

public final class AiGuardPolicy {

    private AiGuardPolicy() {
    }

    public static final int MAX_INPUT_LENGTH = 500;

    public static final List<String> FORBIDDEN_WORDS = List.of(
            "시스템 프롬프트", "system prompt", "ignore previous",
            "너의 지시사항", "프롬프트 무시", "역할을 무시"
    );
}
