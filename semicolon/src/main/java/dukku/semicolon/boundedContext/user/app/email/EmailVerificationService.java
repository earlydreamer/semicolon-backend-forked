package dukku.semicolon.boundedContext.user.app.email;

import dukku.common.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String TOKEN_KEY_PREFIX = "email:verify:token:";
    private static final String VERIFIED_KEY_PREFIX = "email:verify:ok:";

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${custom.email.verification.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${custom.email.verification.token-ttl-seconds:1800}")
    private long tokenTtlSeconds;

    @Value("${custom.email.verification.verified-ttl-seconds:1800}")
    private long verifiedTtlSeconds;

    public void sendVerificationLink(String email) {
        String normalizedEmail = normalizeEmail(email);
        String token = generateToken();
        saveToken(normalizedEmail, token);
        sendMail(normalizedEmail, token);
    }

    public String verifyByToken(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        String email = getStoredEmail(normalizedToken);
        if (email == null) {
            throw new BadRequestException("인증 토큰이 만료되었거나 존재하지 않습니다.");
        }
        redisTemplate.delete(tokenKey(normalizedToken));
        markVerified(email);
        return email;
    }

    public void assertVerifiedForRegister(String email) {
        String normalizedEmail = normalizeEmail(email);
        Object verified = redisTemplate.opsForValue().get(verifiedKey(normalizedEmail));
        if (verified == null) {
            throw new BadRequestException("이메일 인증이 필요합니다.");
        }
        redisTemplate.delete(verifiedKey(normalizedEmail));
    }

    private void saveToken(String email, String token) {
        redisTemplate.opsForValue()
                .set(tokenKey(token), email, Duration.ofSeconds(tokenTtlSeconds));
    }

    private String getStoredEmail(String token) {
        Object stored = redisTemplate.opsForValue().get(tokenKey(token));
        return stored == null ? null : stored.toString();
    }

    private void markVerified(String email) {
        redisTemplate.opsForValue()
                .set(verifiedKey(email), "true", Duration.ofSeconds(verifiedTtlSeconds));
    }

    private void sendMail(String email, String token) {
        String link = buildVerifyLink(token);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("세미콜론 이메일 인증");
        message.setText("아래 링크를 클릭해 이메일 인증을 완료해 주세요.\n" + link);
        if (mailFrom != null && !mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        mailSender.send(message);
    }

    private String buildVerifyLink(String token) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + "/api/v1/users/email/verify?token=" + token;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
