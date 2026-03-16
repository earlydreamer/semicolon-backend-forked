package dukku.user.boundedContext.user.app.email;

import dukku.common.shared.user.exception.UserEmailVerificationRequiredException;
import dukku.common.shared.user.exception.UserEmailVerificationTokenInvalidException;
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
/**
 * 이메일 검증 링크 발송과 검증 결과 토큰 관리를 담당하는 서비스입니다.
 */
public class EmailVerificationService {

    private static final String TOKEN_KEY_PREFIX = "email:verify:token:";
    private static final String VERIFIED_KEY_PREFIX = "email:verify:ok:";
    private static final String RESULT_TOKEN_KEY_PREFIX = "email:verify:result:";

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${custom.email.verification.base-url:https://dukku.earlydreamer.dev}")
    private String baseUrl;

    @Value("${custom.email.verification.token-ttl-seconds:1800}")
    private long tokenTtlSeconds;

    @Value("${custom.email.verification.verified-ttl-seconds:1800}")
    private long verifiedTtlSeconds;

    @Value("${custom.email.verification.result-token-ttl-seconds:300}")
    private long resultTokenTtlSeconds;

    @Value("${custom.email.verification.required:true}")
    private boolean verificationRequired;

    public void sendVerificationLink(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!verificationRequired) {
            markVerified(normalizedEmail);
            return;
        }
        String token = generateToken();
        saveToken(normalizedEmail, token);
        sendMail(normalizedEmail, token);
    }

    public String verifyByToken(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        String email = getStoredEmail(normalizedToken);
        if (email == null) {
            throw new UserEmailVerificationTokenInvalidException();
        }
        redisTemplate.delete(tokenKey(normalizedToken));
        markVerified(email);
        return email;
    }

    public void assertVerifiedForRegister(String email) {
        if (!verificationRequired) {
            return;
        }
        String normalizedEmail = normalizeEmail(email);
        Object verified = redisTemplate.opsForValue().get(verifiedKey(normalizedEmail));
        if (verified == null) {
            throw new UserEmailVerificationRequiredException();
        }
        redisTemplate.delete(verifiedKey(normalizedEmail));
    }

    public String issueVerificationResultToken(boolean verified, String email) {
        // 프론트 인증 결과 페이지 진입용 1회성 결과 토큰 저장
        String token = generateToken();
        String normalizedEmail = normalizeEmail(email);
        String payload = (verified ? "1" : "0") + "|" + normalizedEmail;
        redisTemplate.opsForValue()
                .set(resultTokenKey(token), payload, Duration.ofSeconds(resultTokenTtlSeconds));
        return token;
    }

    public VerificationResult consumeVerificationResultToken(String token) {
        String normalizedToken = token == null ? "" : token.trim();
        Object stored = redisTemplate.opsForValue().get(resultTokenKey(normalizedToken));
        if (stored == null) {
            return VerificationResult.invalid();
        }

        // 토큰 재사용 방지를 위해 조회 즉시 삭제
        redisTemplate.delete(resultTokenKey(normalizedToken));
        String payload = stored.toString();
        String[] parts = payload.split("\\|", 2);
        boolean verified = parts.length > 0 && "1".equals(parts[0]);
        String email = parts.length > 1 ? parts[1] : "";
        return new VerificationResult(verified, email);
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

    private String resultTokenKey(String token) {
        return RESULT_TOKEN_KEY_PREFIX + token;
    }

    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record VerificationResult(boolean verified, String email) {
        public static VerificationResult invalid() {
            return new VerificationResult(false, "");
        }
    }
}
