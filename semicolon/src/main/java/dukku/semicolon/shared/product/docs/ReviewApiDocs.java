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
            name = "판매자 리뷰 API",
            description = "판매자 평점/후기 작성, 조회, 수정, 삭제 API"
    )
    public @interface ReviewTag {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 후기 작성",
            description = "구매 확정된 주문 상품(orderItem)에 대해 판매자 후기를 작성합니다. " +
                    "요청값(orderItemUuid/sellerUuid/productUuid)은 추후 Order 도메인 검증(Feign)으로 보강합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "후기 작성 성공",
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
                                                      "content": "설명과 동일하고 배송도 빨랐어요. 추천합니다!",
                                                      "createdAt": "2026-02-04T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 리뷰가 존재함 (orderItem 기준 중복 작성)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "이미 리뷰가 존재합니다.",
                                                      "details": "해당 주문 상품에 대한 리뷰가 이미 작성되었습니다."
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값 검증 실패",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public @interface CreateProductReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 후기 수정",
            description = "리뷰 UUID로 내용을 수정합니다. (작성자만 가능) 삭제된 리뷰는 수정할 수 없습니다.",
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
                            description = "후기 수정 성공",
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
                                                      "content": "전체적으로 좋았는데 포장이 조금 아쉬웠어요.",
                                                      "createdAt": "2026-02-04T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음(작성자 아님)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "권한이 없습니다.",
                                                      "details": "리뷰 작성자만 수정할 수 있습니다."
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리뷰를 찾을 수 없음(없거나 삭제됨)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "리소스를 찾을 수 없습니다.",
                                                      "details": "존재하지 않는 리뷰입니다."
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "요청값 검증 실패",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public @interface UpdateProductReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 후기 삭제",
            description = "리뷰 UUID로 리뷰를 삭제합니다. (소프트 삭제: deletedAt 설정) 작성자만 가능",
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
                    @ApiResponse(responseCode = "204", description = "후기 삭제 성공"),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음(작성자 아님)",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "리뷰를 찾을 수 없음(없거나 삭제됨)",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public @interface DeleteProductReview {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 평점 조회",
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
                            description = "평점 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Seller Rating",
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
    public @interface FindSellerRating {}

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "판매자 후기 목록 조회",
            description = "판매자의 후기 목록을 페이지네이션으로 조회합니다. 삭제된 리뷰는 content가 '삭제된 후기입니다.'로 마스킹됩니다.",
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
                            description = "정렬(기본 createdAt,desc). 예: createdAt,desc",
                            example = "createdAt,desc"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "후기 목록 조회 성공",
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
                                                          "content": "설명과 동일하고 배송도 빨랐어요. 추천합니다!",
                                                          "createdAt": "2026-02-04T10:00:00"
                                                        },
                                                        {
                                                          "reviewUuid": "22222222-2222-3333-4444-555555555555",
                                                          "sellerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                          "buyerUuid": "88888888-7777-6666-5555-444444444444",
                                                          "orderItemUuid": "4fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "productUuid": "6fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                          "rating": 3,
                                                          "content": "삭제된 후기입니다.",
                                                          "createdAt": "2026-02-03T09:00:00"
                                                        }
                                                      ],
                                                      "page": 0,
                                                      "size": 10,
                                                      "totalCount": 27,
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
    public @interface FindAllSellerReviews {}
}
