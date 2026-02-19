package dukku.common.shared.ai.exception;

public class AiQuestionTooLongException extends AiGuardException {

    public AiQuestionTooLongException(int maxLength) {
        super("질문이 너무 깁니다. 최대 " + maxLength + "자까지 입력 가능합니다.");
    }
}
