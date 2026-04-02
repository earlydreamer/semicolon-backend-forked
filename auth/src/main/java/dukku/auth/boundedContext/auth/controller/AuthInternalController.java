package dukku.auth.boundedContext.auth.controller;

import dukku.auth.boundedContext.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/auth")
@RequiredArgsConstructor
@Hidden
public class AuthInternalController {

    private final AuthService authService;

    @DeleteMapping("/users/{userUuid}/sessions")
    public ResponseEntity<Void> revokeAllSessions(@PathVariable UUID userUuid) {
        authService.revokeAllSessions(userUuid);
        return ResponseEntity.noContent().build();
    }
}
