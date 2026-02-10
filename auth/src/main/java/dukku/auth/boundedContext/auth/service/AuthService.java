package dukku.semicolon.boundedContext.auth.service;

import dukku.common.global.exception.NotFoundException;
import dukku.common.global.exception.UnauthorizedException;
import dukku.semicolon.boundedContext.auth.dto.AccessTokenResponse;
import dukku.semicolon.boundedContext.auth.dto.LoginRequest;
import dukku.semicolon.boundedContext.auth.dto.TokenResponse;
import dukku.semicolon.boundedContext.auth.jwt.AuthTokenIssuer;
import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenStoreService refreshTokenStoreService;

    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new NotFoundException("현재 존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new UnauthorizedException("비밀번호가 올바르지 않습니다.");
        }

        String accessToken = authTokenIssuer.createAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = authTokenIssuer.createRefreshToken(user.getUuid(), user.getRole().name());

        long ttlMillis = authTokenIssuer.getRefreshTokenTtlMillis(refreshToken);
        refreshTokenStoreService.save(user.getUuid(), refreshToken, java.time.Duration.ofMillis(ttlMillis));

        return new TokenResponse(accessToken, refreshToken);
    }

    public AccessTokenResponse refresh(String refreshToken) {
        Claims claims = authTokenIssuer.parseRefreshClaims(refreshToken);
        UUID userUuid = UUID.fromString(claims.getSubject());
        String role = claims.get("ROLE", String.class);

        String storedRefreshToken = refreshTokenStoreService.get(userUuid);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }

        String accessToken = authTokenIssuer.createAccessToken(userUuid, role);
        return new AccessTokenResponse(accessToken);
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