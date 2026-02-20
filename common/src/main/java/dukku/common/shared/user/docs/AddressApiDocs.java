package dukku.common.shared.user.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public final class AddressApiDocs {

    private AddressApiDocs() {
    }

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "Address API",
            description = "My address list and registration endpoints."
    )
    public @interface AddressTag {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "내 배송지 목록 조회",
            description = "로그인한 사용자의 배송지 목록을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\":\"Authentication is required.\"}")
                            )
                    )
            }
    )
    public @interface GetMyAddresses {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "배송지 등록",
            description = "로그인한 사용자의 배송지를 등록합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "등록 성공"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청 값 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\":\"잘못된 요청입니다.\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\":\"Authentication is required.\"}")
                            )
                    )
            }
    )
    public @interface AddAddress {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(summary = "배송지 수정")
    public @interface UpdateAddress {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(summary = "배송지 삭제")
    public @interface DeleteAddress {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(summary = "기본 배송지 변경")
    public @interface SetDefaultAddress {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(summary = "기본 배송지 조회")
    public @interface GetMyDefaultAddress {
    }
}
