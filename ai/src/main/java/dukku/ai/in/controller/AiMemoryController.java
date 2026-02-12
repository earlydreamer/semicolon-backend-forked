package dukku.ai.in.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-memories")
public class AiMemoryController {

    private final AiMemoryFacade aiMemoryFacade;

    public AiMemoryController(AiMemoryFacade aiMemoryFacade) {
        this.aiMemoryFacade = aiMemoryFacade;
    }

    @GetMapping
    public ResponseEntity<List<AiMemoryFacade.AiMemoryResponse>> findAll() {
        return ResponseEntity.ok(aiMemoryFacade.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiMemoryFacade.AiMemoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(aiMemoryFacade.findById(id));
    }

    @PostMapping
    public ResponseEntity<AiMemoryFacade.AiMemoryResponse> create(
            @RequestBody AiMemoryFacade.CreateAiMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aiMemoryFacade.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AiMemoryFacade.AiMemoryResponse> update(
            @PathVariable Long id,
            @RequestBody AiMemoryFacade.UpdateAiMemoryRequest request) {
        return ResponseEntity.ok(aiMemoryFacade.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aiMemoryFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
