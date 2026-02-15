package dukku.auth.boundedContext.auth.service;

import dukku.auth.boundedContext.auth.dto.LoginRequest;
import dukku.auth.boundedContext.auth.dto.TokenResponse;
import dukku.auth.boundedContext.auth.exception.InvalidRefreshTokenException;
import dukku.auth.boundedContext.auth.infra.UserClient;
import dukku.auth.boundedContext.auth.jwt.AuthTokenIssuer;
import dukku.common.global.exception.UnauthorizedException;
import dukku.common.shared.user.dto.UserVerificationResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserClient userClient;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenStoreService refreshTokenStoreService;

    public TokenResponse login(LoginRequest request) {
        UserVerificationResponse user = userClient.verifyUser(request.getEmail(), request.getPassword());

        String accessToken = authTokenIssuer.createAccessToken(user.getUserUuid(), user.getRole().name());
        String refreshToken = authTokenIssuer.createRefreshToken(user.getUserUuid(), user.getRole().name());

        long ttlMillis = authTokenIssuer.getRefreshTokenTtlMillis(refreshToken);
        refreshTokenStoreService.save(user.getUserUuid(), refreshToken, Duration.ofMillis(ttlMillis));

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims = authTokenIssuer.parseRefreshClaims(refreshToken);
        UUID userUuid = UUID.fromString(claims.getSubject());
        String role = claims.get("ROLE", String.class);

        String storedRefreshToken = refreshTokenStoreService.get(userUuid);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            refreshTokenStoreService.delete(userUuid);
            throw new InvalidRefreshTokenException();
        }

        String accessToken = authTokenIssuer.createAccessToken(userUuid, role);
        String newRefreshToken = authTokenIssuer.createRefreshToken(userUuid, role);
        long ttlMillis = authTokenIssuer.getRefreshTokenTtlMillis(newRefreshToken);
        refreshTokenStoreService.save(userUuid, newRefreshToken, Duration.ofMillis(ttlMillis));

        return new TokenResponse(accessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = authTokenIssuer.parseRefreshClaims(refreshToken);
            UUID userUuid = UUID.fromString(claims.getSubject());
            refreshTokenStoreService.delete(userUuid);
        } catch (UnauthorizedException ignored) {
        }
    }
}