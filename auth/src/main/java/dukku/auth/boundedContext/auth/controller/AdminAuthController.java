package dukku.auth.boundedContext.auth.controller;

import dukku.auth.boundedContext.auth.dto.LoginRequest;
import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.service.AuthService;
import dukku.common.shared.auth.docs.AuthApiDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@AuthApiDocs.AuthTag
public class AdminAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @AuthApiDocs.AdminLogin
    public ResponseEntity<TokenResponse> loginAdmin(
            @RequestBody @Validated LoginRequest request
    ) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
