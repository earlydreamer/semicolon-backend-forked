package dukku.auth.boundedContext.auth.jwt;

import dukku.auth.boundedContext.auth.exception.InvalidRefreshTokenException;
import dukku.common.global.auth.jwt.JwtTokenUtil;
import dukku.common.shared.user.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class AuthTokenIssuer {

    private static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 60L * 24;
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7L;
    private static final long REFRESH_TOKEN_ABSOLUTE_VALIDITY = 1000 * 60 * 60 * 24 * 14L;
    private static final String CLAIM_ROLE = "ROLE";
    private static final String CLAIM_ABSOLUTE_EXP = "ABS_EXP";

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final JwtTokenUtil jwtValidator;

    public AuthTokenIssuer(
            @Value("${jwt.access.secret.key}") String accessSecret,
            @Value("${jwt.refresh.secret.key}") String refreshSecret,
            JwtTokenUtil jwtValidator
    ) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
        this.jwtValidator = jwtValidator;
    }

    public String issue(UUID userUuid, Role role) {
        return createToken(userUuid, role.name(), ACCESS_TOKEN_VALIDITY, accessKey);
    }

    private String createToken(UUID userUuid, String role, long validity, SecretKey key) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity);

        return Jwts.builder()
                .subject(userUuid.toString())
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createAccessToken(UUID userUuid, String role) {
        return createToken(userUuid, role, ACCESS_TOKEN_VALIDITY, accessKey);
    }

    public String createRefreshToken(UUID userUuid, String role) {
        long absoluteExpiryMillis = System.currentTimeMillis() + REFRESH_TOKEN_ABSOLUTE_VALIDITY;
        return createRefreshToken(userUuid, role, absoluteExpiryMillis);
    }

    public String createRefreshToken(UUID userUuid, String role, long absoluteExpiryMillis) {
        long nowMillis = System.currentTimeMillis();
        long slidingExpiryMillis = nowMillis + REFRESH_TOKEN_VALIDITY;
        long refreshExpiryMillis = Math.min(slidingExpiryMillis, absoluteExpiryMillis);

        if (refreshExpiryMillis <= nowMillis) {
            throw new InvalidRefreshTokenException();
        }

        Date now = new Date(nowMillis);
        Date expiry = new Date(refreshExpiryMillis);

        return Jwts.builder()
                .subject(userUuid.toString())
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_ABSOLUTE_EXP, absoluteExpiryMillis)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(refreshKey)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(refreshKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("리프레시 토큰 검증에 실패했습니다: {}", e.getMessage());
            return false;
        }
    }

    public Claims parseRefreshClaims(String refreshToken) {
        try {
            return Jwts.parser()
                    .verifyWith(refreshKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("리프레시 토큰 검증에 실패했습니다: {}", e.getMessage());
            throw new InvalidRefreshTokenException();
        }
    }

    public long getRefreshTokenTtlMillis(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);
        Date expiration = claims.getExpiration();
        long ttl = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, ttl);
    }

    public long getRefreshTokenAbsoluteExpiryMillis(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);
        Long absoluteExpiry = claims.get(CLAIM_ABSOLUTE_EXP, Long.class);
        if (absoluteExpiry != null) {
            return absoluteExpiry;
        }
        return claims.getExpiration().getTime();
    }

    public String refresh(String refreshToken) {
        Claims claims = parseRefreshClaims(refreshToken);

        UUID userUuid = UUID.fromString(claims.getSubject());
        String role = claims.get(CLAIM_ROLE, String.class);

        return createAccessToken(userUuid, role);
    }
}

