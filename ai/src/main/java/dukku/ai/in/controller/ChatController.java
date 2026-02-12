package dukku.ai.in.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatFacade chatFacade;

    public ChatController(ChatFacade chatFacade) {
        this.chatFacade = chatFacade;
    }

    @PostMapping
    public ResponseEntity<ChatFacade.ChatResponse> chat(@RequestBody ChatFacade.ChatRequest request) {
        ChatFacade.ChatResponse response = chatFacade.chat(request);
        return ResponseEntity.ok(response);
    }

}
