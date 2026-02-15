package dukku.common.shared.ai.exception;

public class AiForbiddenWordIncludedException extends AiGuardException {

    public AiForbiddenWordIncludedException(String word) {
        super("금칙어가 포함되어 있습니다: " + word);
    }
}
