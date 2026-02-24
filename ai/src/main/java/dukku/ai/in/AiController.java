package dukku.ai.in;

import java.util.List;
import java.util.UUID;

import dukku.ai.app.AiFacade;
import dukku.common.shared.ai.docs.AiApiDocs;
import dukku.common.shared.ai.dto.AiUserMemoryResponse;
import dukku.common.shared.ai.dto.ChatRequest;
import dukku.common.shared.ai.dto.CreateAiUserMemoryRequest;
import dukku.common.shared.ai.dto.UpdateAiUserMemoryRequest;
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
    @GetMapping("/ai-user-memories")
    public ResponseEntity<List<AiUserMemoryResponse>> findAllMemories() {
        return ResponseEntity.ok(aiFacade.findAll());
    }

    @AiApiDocs.FindMemoryById
    @GetMapping("/ai-user-memories/{id}")
    public ResponseEntity<AiUserMemoryResponse> findMemoryById(@PathVariable("id") Integer aiUserMemoryId) {
        return ResponseEntity.ok(aiFacade.findById(aiUserMemoryId));
    }

    @AiApiDocs.CreateMemory
    @PostMapping("/ai-user-memories")
    public ResponseEntity<AiUserMemoryResponse> createMemory(@RequestBody CreateAiUserMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiFacade.create(request));
    }

    @AiApiDocs.UpdateMemory
    @PatchMapping("/ai-user-memories/{id}")
    public ResponseEntity<AiUserMemoryResponse> updateMemory(
            @PathVariable("id") Integer aiUserMemoryId,
            @RequestBody UpdateAiUserMemoryRequest request) {
        return ResponseEntity.ok(aiFacade.update(aiUserMemoryId, request));
    }

    @AiApiDocs.DeleteMemory
    @DeleteMapping("/ai-user-memories/{id}")
    public ResponseEntity<Void> deleteMemory(@PathVariable("id") Integer aiUserMemoryId) {
        aiFacade.delete(aiUserMemoryId);
        return ResponseEntity.noContent().build();
    }

    @AiApiDocs.FindRecommendations
    @GetMapping("/recommendations/{userUuid}")
    public ResponseEntity<List<AiUserMemoryResponse>> findRecommendations(@PathVariable("userUuid") UUID userUuid) {
        return ResponseEntity.ok(aiFacade.findRecommendations(userUuid));
    }
}
