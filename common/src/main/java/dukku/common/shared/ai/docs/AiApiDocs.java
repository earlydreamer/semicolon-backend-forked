package dukku.common.shared.ai.docs;

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

public final class AiApiDocs {

    private AiApiDocs() {
    }

    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @Tag(
            name = "AI 채팅 및 장기 기억 관리 API",
            description = "AI 모델과의 채팅, 사용자 장기 기억 CRUD 관련 기능"
    )
    public @interface AiTag {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 채팅",
            description = "AI 모델과 대화합니다. 사용자 정보 기반 상품 추천을 받을 수 있습니다. 스트리밍(SSE) 방식으로 응답합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "채팅 요청",
                                    value = """
                                            {
                                              "conversationId": "conv-123",
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "message": "가성비 좋은 노트북 추천해줘"
                                            }"""
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "스트리밍 응답 성공 (text/event-stream)"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력 검증 실패 (금칙어 포함 또는 최대 길이 초과)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "AI 입력 검증에 실패했습니다. 질문이 너무 깁니다. 최대 500자까지 입력 가능합니다."}""")
                            )
                    )
            }
    )
    public @interface Chat {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 메모리 전체 조회",
            description = "저장된 모든 AI 장기 기억을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "메모리 목록",
                                            value = """
                                                    [
                                                      {
                                                        "id": 1,
                                                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                                                        "memoryType": "PROFILE",
                                                        "subType": "GENERAL",
                                                        "content": "사용자는 백엔드 개발자입니다",
                                                        "importanceScore": 0.9,
                                                        "confidenceScore": 0.85,
                                                        "accessCount": 3,
                                                        "createdAt": "2026-02-13T10:00:00",
                                                        "updatedAt": "2026-02-13T12:00:00"
                                                      }
                                                    ]"""
                                    )
                            )
                    )
            }
    )
    public @interface FindAllMemories {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 메모리 단건 조회",
            description = "ID로 특정 AI 장기 기억을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 메모리 ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "AI 메모리를 찾을 수 없습니다. id=999"}""")
                            )
                    )
            }
    )
    public @interface FindMemoryById {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 메모리 생성",
            description = "새로운 AI 장기 기억을 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "메모리 생성 요청",
                                    value = """
                                            {
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "memoryType": "PREFERENCE",
                                              "subType": "SHOPPING",
                                              "content": "가성비 제품을 선호합니다",
                                              "importanceScore": 0.8,
                                              "confidenceScore": 0.7
                                            }"""
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "생성 성공"
                    )
            }
    )
    public @interface CreateMemory {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 메모리 수정",
            description = "기존 AI 장기 기억의 중요도/신뢰도 점수를 수정합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "메모리 수정 요청",
                                    value = """
                                            {
                                              "importanceScore": 0.95,
                                              "confidenceScore": 0.88
                                            }"""
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 메모리 ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "AI 메모리를 찾을 수 없습니다. id=999"}""")
                            )
                    )
            }
    )
    public @interface UpdateMemory {
    }

    @Documented
    @Target(METHOD)
    @Retention(RUNTIME)
    @Operation(
            summary = "AI 메모리 삭제",
            description = "특정 AI 장기 기억을 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "삭제 성공"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 메모리 ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = """
                                            {"message": "AI 메모리를 찾을 수 없습니다. id=999"}""")
                            )
                    )
            }
    )
    public @interface DeleteMemory {
    }
}
