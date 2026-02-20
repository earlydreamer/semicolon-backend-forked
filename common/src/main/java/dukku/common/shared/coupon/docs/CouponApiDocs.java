package dukku.common.shared.coupon.docs;

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

public final class CouponApiDocs {

    private CouponApiDocs() {
    }

    // =============== 공통 태그 ===============
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "쿠폰 관리 API",
            description = "쿠폰 발급, 사용, 조회 관련 기능"
    )
    public @interface CouponTag {
    }

    // =============== 1) 쿠폰 발급 (유저) ===============
    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "쿠폰 발급",
            description = "사용자가 특정 쿠폰을 발급받습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "쿠폰 발급 성공 (No Content)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "쿠폰을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\": \"해당 쿠폰을 찾을 수 없습니다.\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "발급 조건 불충족 (재고 소진, 기간 만료 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\": \"쿠폰 발급이 불가능합니다.\"}")
                            )
                    )
            }
    )
    public @interface IssueCoupon {
    }

    // =============== 2) 쿠폰 사용 (유저) ===============
    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "쿠폰 사용",
            description = "사용자가 보유한 쿠폰을 사용합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "쿠폰 사용 성공 (No Content)"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "보유한 쿠폰을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\": \"보유한 쿠폰을 찾을 수 없습니다.\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "사용 불가 (이미 사용됨, 만료됨 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{\"message\": \"쿠폰을 사용할 수 없습니다.\"}")
                            )
                    )
            }
    )
    public @interface UseCoupon {
    }

    // =============== 3) 발급 가능한 쿠폰 리스트 (유저) ===============
    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "발급 가능한 쿠폰 조회",
            description = "현재 사용자가 발급받을 수 있는 쿠폰 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Issuable Coupon List",
                                            value = """
                                                    [
                                                      {
                                                        "couponUuid": "550e8400-e29b-41d4-a716-446655440001",
                                                        "name": "오픈 기념 10% 할인 쿠폰",
                                                        "discountType": "PERCENTAGE",
                                                        "discountValue": 10,
                                                        "validFrom": "2024-01-01T00:00:00",
                                                        "validUntil": "2024-12-31T23:59:59"
                                                      },
                                                      {
                                                        "couponUuid": "550e8400-e29b-41d4-a716-446655440002",
                                                        "name": "신규 가입 5000원 할인 쿠폰",
                                                        "discountType": "FIXED_AMOUNT",
                                                        "discountValue": 5000,
                                                        "validFrom": "2024-01-01T00:00:00",
                                                        "validUntil": "2024-01-31T23:59:59"
                                                      }
                                                    ]
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface FindIssuableCoupons {
    }

    // =============== 4) 내가 보유한 쿠폰 리스트 (유저) ===============
    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "보유 쿠폰 조회",
            description = "사용자가 현재 보유하고 있는 쿠폰 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "My Coupon List",
                                            value = """
                                                    [
                                                      {
                                                        "couponUuid": "550e8400-e29b-41d4-a716-446655440001",
                                                        "name": "오픈 기념 10% 할인 쿠폰",
                                                        "discountType": "PERCENTAGE",
                                                        "discountValue": 10,
                                                        "validFrom": "2024-01-01T00:00:00",
                                                        "validUntil": "2024-12-31T23:59:59",
                                                        "status": "UNUSED"
                                                      }
                                                    ]
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface FindMyCoupons {
    }
}
