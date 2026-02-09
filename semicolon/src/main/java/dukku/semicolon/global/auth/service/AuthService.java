package dukku.semicolon.global.auth.service;

import dukku.common.global.exception.NotFoundException;
import dukku.common.global.exception.UnauthorizedException;
import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import dukku.semicolon.global.auth.dto.AccessTokenResponse;
import dukku.semicolon.global.auth.dto.LoginRequest;
import dukku.semicolon.global.auth.dto.RefreshTokenRequest;
import dukku.semicolon.global.auth.dto.TokenResponse;
import dukku.semicolon.global.auth.jwt.AuthTokenIssuer;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenBlacklistService refreshTokenBlacklistService;

    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() ->  new NotFoundException("존재하지 않는 회원입니다."));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new UnauthorizedException("비밀번호가 올바르지 않습니다.");
        }

        String accessToken = authTokenIssuer.createAccessToken(
                user.getUuid(),
                user.getRole().name()
        );
        String refreshToken = authTokenIssuer.createRefreshToken(
                user.getUuid(),
                user.getRole().name()
        );

        return new TokenResponse(accessToken, refreshToken);
    }

    public AccessTokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (refreshTokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }

        Claims claims = authTokenIssuer.parseRefreshClaims(refreshToken);
        UUID userUuid = UUID.fromString(claims.getSubject());
        String role = claims.get("ROLE", String.class);

        String accessToken = authTokenIssuer.createAccessToken(userUuid, role);
        return new AccessTokenResponse(accessToken);
    }

    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        long ttlMillis = authTokenIssuer.getRefreshTokenTtlMillis(refreshToken);
        refreshTokenBlacklistService.blacklist(refreshToken, Duration.ofMillis(ttlMillis));
    }
}
