package dukku.ai.in;

import java.util.List;

import dukku.ai.app.AiFacade;
import dukku.common.shared.ai.dto.AiMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiMemoryRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI", description = "AI 채팅 및 장기 기억 관리 API")
@RestController
@RequestMapping("/api")
public class AiController {

    private final AiFacade aiFacade;

    public AiController(AiFacade aiFacade) {
        this.aiFacade = aiFacade;
    }

    // ========== Chat ==========

    @Operation(summary = "AI 채팅", description = "AI 모델과 대화합니다. 사용자 정보 기반 상품 추천을 받을 수 있습니다. 스트리밍 방식으로 응답합니다.")
    @ApiResponse(responseCode = "200", description = "응답 성공")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return aiFacade.chat(request);
    }

    // ========== AI Memory ==========

    @Operation(summary = "AI 메모리 전체 조회", description = "저장된 모든 AI 장기 기억을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/ai-memories")
    public ResponseEntity<List<AiMemoryResponse>> findAllMemories() {
        return ResponseEntity.ok(aiFacade.findAll());
    }

    @Operation(summary = "AI 메모리 단건 조회", description = "ID로 특정 AI 장기 기억을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 메모리 ID")
    @GetMapping("/ai-memories/{id}")
    public ResponseEntity<AiMemoryResponse> findMemoryById(
            @Parameter(description = "메모리 ID") @PathVariable Long id) {
        return ResponseEntity.ok(aiFacade.findById(id));
    }

    @Operation(summary = "AI 메모리 생성", description = "새로운 AI 장기 기억을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @PostMapping("/ai-memories")
    public ResponseEntity<AiMemoryResponse> createMemory(
            @RequestBody CreateAiMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiFacade.create(request));
    }

    @Operation(summary = "AI 메모리 수정", description = "기존 AI 장기 기억의 중요도/신뢰도 점수를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 메모리 ID")
    @PatchMapping("/ai-memories/{id}")
    public ResponseEntity<AiMemoryResponse> updateMemory(
            @Parameter(description = "메모리 ID") @PathVariable Long id,
            @RequestBody UpdateAiMemoryRequest request) {
        return ResponseEntity.ok(aiFacade.update(id, request));
    }

    @Operation(summary = "AI 메모리 삭제", description = "특정 AI 장기 기억을 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @ApiResponse(responseCode = "400", description = "존재하지 않는 메모리 ID")
    @DeleteMapping("/ai-memories/{id}")
    public ResponseEntity<Void> deleteMemory(
            @Parameter(description = "메모리 ID") @PathVariable Long id) {
        aiFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
