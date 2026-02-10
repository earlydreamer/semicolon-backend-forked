package dukku.semicolon.global.auth.service;

import dukku.common.global.exception.NotFoundException;
import dukku.common.global.exception.UnauthorizedException;
import dukku.semicolon.boundedContext.user.entity.User;
import dukku.semicolon.boundedContext.user.out.UserRepository;
import dukku.semicolon.global.auth.dto.AccessTokenResponse;
import dukku.semicolon.global.auth.dto.LoginRequest;
import dukku.semicolon.global.auth.dto.LoginTokens;
import dukku.semicolon.global.auth.jwt.AuthTokenIssuer;
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

    public LoginTokens login(LoginRequest request) {

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() ->  new NotFoundException("議댁옱?섏? ?딅뒗 ?뚯썝?낅땲??"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new UnauthorizedException("鍮꾨?踰덊샇媛 ?щ컮瑜댁? ?딆뒿?덈떎.");
        }

        String accessToken = authTokenIssuer.createAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = authTokenIssuer.createRefreshToken(user.getUuid(), user.getRole().name());

        long ttlMillis = authTokenIssuer.getRefreshTokenTtlMillis(refreshToken);
        refreshTokenStoreService.save(user.getUuid(), refreshToken, java.time.Duration.ofMillis(ttlMillis));

        return new LoginTokens(accessToken, refreshToken);
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

    public long getRefreshTokenTtlMillis(String refreshToken) {
        return authTokenIssuer.getRefreshTokenTtlMillis(refreshToken);
    }
}
