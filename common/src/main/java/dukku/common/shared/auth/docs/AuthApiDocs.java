package dukku.common.shared.auth.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public final class AuthApiDocs {

    private AuthApiDocs() {
    }

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "Auth API",
            description = "로그인/토큰 재발급/로그아웃 및 소셜 로그인 진입 API"
    )
    public @interface AuthTag {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "이메일 로그인",
            description = "이메일/비밀번호를 검증하고 access/refresh 토큰을 발급합니다.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "email": "semicolon@example.com",
                                              "password": "Password123!"
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공"),
                    @ApiResponse(responseCode = "401", description = "인증 실패")
            }
    )
    public @interface Login {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "구글 소셜 로그인 시작",
            description = "구글 OAuth 인증 페이지로 리다이렉트합니다. " +
                    "브라우저에서 호출해야 하며, 인증 완료 후 콜백 URL로 이동합니다.",
            responses = {
                    @ApiResponse(responseCode = "302", description = "구글 인증 페이지로 리다이렉트"),
                    @ApiResponse(responseCode = "500", description = "리다이렉트 생성 실패")
            }
    )
    public @interface StartGoogleSocialLogin {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "토큰 재발급",
            description = "헤더 `X-Refresh-Token`의 refresh token으로 access/refresh를 재발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "재발급 성공"),
                    @ApiResponse(responseCode = "401", description = "유효하지 않은 refresh token")
            }
    )
    public @interface Refresh {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "로그아웃",
            description = "헤더 `X-Refresh-Token` 기반으로 서버에 저장된 refresh token을 폐기합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
            }
    )
    public @interface Logout {
    }
}
