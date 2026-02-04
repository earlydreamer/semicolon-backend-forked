package dukku.semicolon.shared.product.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

public final class ReviewApiDocs {

    private ReviewApiDocs() {}

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "상점 리뷰 API",
            description = "판매자(상점) 리뷰 작성/수정/삭제 및 판매자 리뷰 목록/요약 조회 API"
    )
    public @interface ReviewTag {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 리뷰 작성",
            description = "구매자가 거래 완료된 주문 상품(orderItemUuid)에 대해 판매자 리뷰를 작성합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "리뷰 작성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Created Seller Review",
                                            value = """
                                                    {
                                                      "reviewUuid": "11111111-2222-3333-4444-555555555555",
                                                      "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                      "buyerUuid": "99999999-8888-7777-6666-555555555555",
                                                      "orderItemUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "productUuid": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "rating": 5,
                                                      "content": "친절하고 배송이 빨라요!",
                                                      "createdAt": "2026-02-04T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "409", description = "이미 해당 주문 상품에 대한 리뷰가 존재함"),
                    @ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
            }
    )
    public @interface CreateSellerReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 리뷰 수정",
            description = "리뷰 UUID로 리뷰 내용을 수정합니다. (작성자만 가능, 삭제된 리뷰는 수정 불가)",
            parameters = {
                    @Parameter(
                            name = "reviewUuid",
                            in = ParameterIn.PATH,
                            description = "리뷰 UUID",
                            required = true,
                            example = "11111111-2222-3333-4444-555555555555"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "리뷰 수정 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Updated Seller Review",
                                            value = """
                                                    {
                                                      "reviewUuid": "11111111-2222-3333-4444-555555555555",
                                                      "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                      "buyerUuid": "99999999-8888-7777-6666-555555555555",
                                                      "orderItemUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "productUuid": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "rating": 4,
                                                      "content": "응답이 빨라서 좋았어요.",
                                                      "createdAt": "2026-02-04T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "403", description = "권한 없음(작성자 아님)"),
                    @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음(삭제 포함)"),
                    @ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
            }
    )
    public @interface UpdateSellerReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 리뷰 삭제",
            description = "리뷰 UUID로 리뷰를 삭제합니다. (소프트 삭제: deletedAt 설정, 작성자만 가능) " +
                    "삭제된 리뷰는 목록에서 '삭제된 후기입니다.'로 마스킹됩니다.",
            parameters = {
                    @Parameter(
                            name = "reviewUuid",
                            in = ParameterIn.PATH,
                            description = "리뷰 UUID",
                            required = true,
                            example = "11111111-2222-3333-4444-555555555555"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "리뷰 삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음(작성자 아님)"),
                    @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음(삭제 포함)")
            }
    )
    public @interface DeleteSellerReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 리뷰 요약 조회",
            description = "판매자의 평균 평점(avgRating)과 후기 수(reviewCount)를 조회합니다. (삭제된 리뷰 제외)",
            parameters = {
                    @Parameter(
                            name = "sellerUuid",
                            in = ParameterIn.PATH,
                            description = "판매자 UUID",
                            required = true,
                            example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "리뷰 요약 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Seller Review Summary",
                                            value = """
                                                    {
                                                      "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                      "avgRating": 4.7,
                                                      "reviewCount": 23
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface FindSellerReviewSummary {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 리뷰 목록 조회",
            description = "판매자의 리뷰 목록을 페이지 단위로 조회합니다. (삭제된 리뷰는 마스킹되어 반환)",
            parameters = {
                    @Parameter(
                            name = "sellerUuid",
                            in = ParameterIn.PATH,
                            description = "판매자 UUID",
                            required = true,
                            example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                    ),
                    @Parameter(
                            name = "page",
                            in = ParameterIn.QUERY,
                            description = "페이지(0부터 시작, 기본 0)",
                            example = "0"
                    ),
                    @Parameter(
                            name = "size",
                            in = ParameterIn.QUERY,
                            description = "페이지 사이즈(기본 10)",
                            example = "10"
                    ),
                    @Parameter(
                            name = "sort",
                            in = ParameterIn.QUERY,
                            description = "정렬 기준 (예: createdAt,desc)",
                            example = "createdAt,desc"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "판매자 리뷰 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Seller Reviews Page",
                                            value = """
                                                    {
                                                      "items": [
                                                        {
                                                          "reviewUuid": "11111111-2222-3333-4444-555555555555",
                                                          "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                          "buyerUuid": "99999999-8888-7777-6666-555555555555",
                                                          "orderItemUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "productUuid": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "rating": 5,
                                                          "content": "친절하고 배송이 빨라요!",
                                                          "createdAt": "2026-02-04T10:00:00"
                                                        },
                                                        {
                                                          "reviewUuid": "22222222-2222-3333-4444-555555555555",
                                                          "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                          "buyerUuid": "88888888-8888-7777-6666-555555555555",
                                                          "orderItemUuid": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "productUuid": "9fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "rating": 3,
                                                          "content": "삭제된 후기입니다.",
                                                          "createdAt": "2026-02-03T10:00:00"
                                                        }
                                                      ],
                                                      "page": 0,
                                                      "size": 10,
                                                      "totalCount": 23,
                                                      "hasNext": true,
                                                      "avgRating": 4.7,
                                                      "reviewCount": 23
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface findSellerReviewList {}
}
