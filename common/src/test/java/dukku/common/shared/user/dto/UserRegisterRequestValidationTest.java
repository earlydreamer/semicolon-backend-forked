package dukku.common.shared.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegisterRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("닉네임이 비어있으면 검증에 실패한다")
    void failsWhenNicknameBlank() {
        UserRegisterRequest request = new UserRegisterRequest("test@example.com", "abc12345", " ");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("닉네임은 필수입니다.");
    }

    @Test
    @DisplayName("닉네임이 50자를 초과하면 검증에 실패한다")
    void failsWhenNicknameTooLong() {
        UserRegisterRequest request = new UserRegisterRequest(
                "test@example.com",
                "abc12345",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("닉네임은 50자 이하여야 합니다.");
    }
}

