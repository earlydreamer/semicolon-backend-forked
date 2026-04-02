package dukku.auth.boundedContext.auth.jwt;

import dukku.common.global.auth.jwt.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenIssuerTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String ROLE = "USER";
    private static final String ACCESS_SECRET = base64Key("0123456789abcdef0123456789abcdef");
    private static final String REFRESH_SECRET = base64Key("fedcba9876543210fedcba9876543210");

    private final JwtTokenUtil jwtValidator = new JwtTokenUtil(ACCESS_SECRET);
    private final AuthTokenIssuer issuer = new AuthTokenIssuer(
            ACCESS_SECRET,
            REFRESH_SECRET,
            jwtValidator,
            300_000L,
            604_800_000L,
            1_209_600_000L
    );

    @Test
    void createAccessToken_usesConfiguredTtl() {
        String token = issuer.createAccessToken(USER_UUID, ROLE);

        Claims claims = parseClaims(token, ACCESS_SECRET);
        long ttl = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(ttl).isBetween(299_000L, 301_000L);
    }

    @Test
    void createRefreshToken_preservesAbsoluteExpiry() {
        long absoluteExpiryMillis = System.currentTimeMillis() + 1_209_600_000L;

        String token = issuer.createRefreshToken(USER_UUID, ROLE, absoluteExpiryMillis);

        assertThat(issuer.getRefreshTokenAbsoluteExpiryMillis(token)).isEqualTo(absoluteExpiryMillis);
    }

    private static Claims parseClaims(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static String base64Key(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
