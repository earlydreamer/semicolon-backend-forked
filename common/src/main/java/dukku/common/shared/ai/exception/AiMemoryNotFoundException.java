package dukku.common.shared.ai.exception;

import dukku.common.global.exception.NotFoundException;

public class AiMemoryNotFoundException extends NotFoundException {

    public AiMemoryNotFoundException(Integer aiMemoryId) {
        super("AI 메모리를 찾을 수 없습니다. id=" + aiMemoryId);
    }
}
