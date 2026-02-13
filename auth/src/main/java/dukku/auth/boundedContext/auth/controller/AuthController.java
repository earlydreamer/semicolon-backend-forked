package dukku.auth.boundedContext.auth.controller;

import dukku.auth.boundedContext.auth.dto.LoginRequest;
import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<TokenResponse> refresh(
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
