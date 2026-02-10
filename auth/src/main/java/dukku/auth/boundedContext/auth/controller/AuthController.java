package dukku.semicolon.boundedContext.auth.controller;

import dukku.semicolon.boundedContext.auth.dto.AccessTokenResponse;
import dukku.semicolon.boundedContext.auth.dto.LoginRequest;
import dukku.semicolon.boundedContext.auth.dto.TokenResponse;
import dukku.semicolon.boundedContext.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Validated LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}
