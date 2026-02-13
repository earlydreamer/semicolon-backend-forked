package dukku.common.shared.ai.exception;

import dukku.common.global.exception.BadRequestException;

public class AiChatException extends BadRequestException {

    public AiChatException(String details) {
        super("AI 채팅 처리 중 오류가 발생했습니다. " + details);
    }
}
