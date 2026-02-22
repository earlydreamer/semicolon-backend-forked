package dukku.ai.in;

import java.util.List;
import java.util.UUID;

import dukku.ai.app.AiFacade;
import dukku.common.shared.ai.docs.AiApiDocs;
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

@AiApiDocs.AiTag
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiFacade aiFacade;

    public AiController(AiFacade aiFacade) {
        this.aiFacade = aiFacade;
    }

    @AiApiDocs.Chat
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return aiFacade.chat(request);
    }

    @AiApiDocs.FindAllMemories
    @GetMapping("/ai-memories")
    public ResponseEntity<List<AiMemoryResponse>> findAllMemories() {
        return ResponseEntity.ok(aiFacade.findAll());
    }

    @AiApiDocs.FindMemoryById
    @GetMapping("/ai-memories/{id}")
    public ResponseEntity<AiMemoryResponse> findMemoryById(@PathVariable("id") Integer aiMemoryId) {
        return ResponseEntity.ok(aiFacade.findById(aiMemoryId));
    }

    @AiApiDocs.CreateMemory
    @PostMapping("/ai-memories")
    public ResponseEntity<AiMemoryResponse> createMemory(@RequestBody CreateAiMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiFacade.create(request));
    }

    @AiApiDocs.UpdateMemory
    @PatchMapping("/ai-memories/{id}")
    public ResponseEntity<AiMemoryResponse> updateMemory(
            @PathVariable("id") Integer aiMemoryId,
            @RequestBody UpdateAiMemoryRequest request) {
        return ResponseEntity.ok(aiFacade.update(aiMemoryId, request));
    }

    @AiApiDocs.DeleteMemory
    @DeleteMapping("/ai-memories/{id}")
    public ResponseEntity<Void> deleteMemory(@PathVariable("id") Integer aiMemoryId) {
        aiFacade.delete(aiMemoryId);
        return ResponseEntity.noContent().build();
    }

    @AiApiDocs.FindRecommendations
    @GetMapping("/recommendations/{userUuid}")
    public ResponseEntity<List<AiMemoryResponse>> findRecommendations(@PathVariable("userUuid") UUID userUuid) {
        return ResponseEntity.ok(aiFacade.findRecommendations(userUuid));
    }
}
