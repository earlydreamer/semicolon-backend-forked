package dukku.common.shared.product.docs;

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

public final class CommentApiDocs {

    private CommentApiDocs() {
    }

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "상품 댓글 API",
            description = "상품 댓글/대댓글 작성, 조회, 수정, 삭제 API"
    )
    public @interface CommentTag {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "댓글 작성",
            description = "특정 상품에 부모 댓글을 작성합니다.",
            parameters = {
                    @Parameter(
                            name = "productUuid",
                            in = ParameterIn.PATH,
                            description = "상품 UUID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 작성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Created Comment",
                                            value = """
                                                    {
                                                      "commentUuid": "11111111-2222-3333-4444-555555555555",
                                                      "productUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "authorUuid": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "authorRole": "BUYER",
                                                      "content": "구매 가능할까요?",
                                                      "parentCommentUuid": null
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "message": "리소스를 찾을 수 없습니다.",
                                                      "details": "존재하지 않는 상품입니다."
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface CreateComment {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "대댓글 작성",
            description = "특정 상품의 부모 댓글에 대댓글을 작성합니다.",
            parameters = {
                    @Parameter(
                            name = "productUuid",
                            in = ParameterIn.PATH,
                            description = "상품 UUID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    ),
                    @Parameter(
                            name = "parentCommentUuid",
                            in = ParameterIn.PATH,
                            description = "부모 댓글 UUID",
                            required = true,
                            example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "대댓글 작성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Created Reply",
                                            value = """
                                                    {
                                                      "commentUuid": "bbbbbbbb-2222-3333-4444-555555555555",
                                                      "productUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "authorUuid": "7fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "authorRole": "SELLER",
                                                      "content": "네, 가능합니다!",
                                                      "parentCommentUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface CreateReply {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "상품 댓글 목록 조회",
            description = "특정 상품의 댓글(부모+대댓글)을 페이징으로 조회합니다.",
            parameters = {
                    @Parameter(
                            name = "page",
                            in = ParameterIn.QUERY,
                            description = "페이지(0부터 시작, 기본 0)",
                            example = "0"
                    ),
                    @Parameter(
                            name = "size",
                            in = ParameterIn.QUERY,
                            description = "페이지 사이즈(기본 20, 최대 50)",
                            example = "20"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Comments List",
                                            value = """
                                                    {
                                                      "items": [
                                                        {
                                                          "parent": {
                                                            "commentUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                            "productUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                            "authorUuid": "11111111-2222-3333-4444-555555555555",
                                                            "authorRole": "BUYER",
                                                            "content": "구매 가능할까요?",
                                                            "parentCommentUuid": null
                                                          },
                                                          "replies": [
                                                            {
                                                              "commentUuid": "bbbbbbbb-2222-3333-4444-555555555555",
                                                              "productUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                              "authorUuid": "99999999-8888-7777-6666-555555555555",
                                                              "authorRole": "SELLER",
                                                              "content": "네 가능합니다!",
                                                              "parentCommentUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                                                            }
                                                          ]
                                                        }
                                                      ],
                                                      "page": 0,
                                                      "size": 20,
                                                      "totalCount": 1,
                                                      "hasNext": false
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public @interface FindCommentsList {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "댓글 수정",
            description = "댓글 UUID로 댓글 내용을 수정합니다. (작성자만 가능)",
            parameters = {
                    @Parameter(
                            name = "productUuid",
                            in = ParameterIn.PATH,
                            description = "상품 UUID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    ),
                    @Parameter(
                            name = "commentUuid",
                            in = ParameterIn.PATH,
                            description = "댓글 UUID",
                            required = true,
                            example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "댓글 수정 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Updated Comment",
                                            value = """
                                                    {
                                                      "commentUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                                      "productUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                      "authorUuid": "11111111-2222-3333-4444-555555555555",
                                                      "authorRole": "BUYER",
                                                      "content": "가격 네고 가능할까요?",
                                                      "parentCommentUuid": null
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "권한 없음(작성자 아님)",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "댓글을 찾을 수 없음",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public @interface UpdateComment {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "댓글 삭제",
            description = "댓글 UUID로 댓글을 삭제합니다. (소프트 삭제 권장: deletedAt 설정) 작성자만 가능",
            parameters = {
                    @Parameter(
                            name = "productUuid",
                            in = ParameterIn.PATH,
                            description = "상품 UUID",
                            required = true,
                            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                    ),
                    @Parameter(
                            name = "commentUuid",
                            in = ParameterIn.PATH,
                            description = "댓글 UUID",
                            required = true,
                            example = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
                    @ApiResponse(responseCode = "403", description = "권한 없음(작성자 아님)"),
                    @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
            }
    )
    public @interface DeleteComment {
    }
}
