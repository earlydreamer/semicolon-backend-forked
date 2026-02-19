package dukku.common.shared.ai.exception;

import dukku.common.global.exception.BadRequestException;

public class AiGuardException extends BadRequestException {

    public AiGuardException(String details) {
        super("AI 입력 검증에 실패했습니다. " + details);
    }
}
