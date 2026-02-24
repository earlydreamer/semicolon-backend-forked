package dukku.user.boundedContext.user.in;

import dukku.common.shared.user.exception.UserEmailVerificationTokenInvalidException;
import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.in.dto.EmailSendRequest;
import dukku.user.boundedContext.user.in.dto.EmailVerifyResponse;
import dukku.user.boundedContext.user.in.dto.EmailVerifyResultRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/users/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Value("${custom.email.verification.success-redirect-url:https://localhost/email/verify}")
    private String successRedirectUrl;

    @PostMapping("/send")
    public ResponseEntity<Void> sendVerificationCode(
            @RequestBody @Validated EmailSendRequest request
    ) {
        emailVerificationService.sendVerificationLink(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(
            @RequestParam("token") String token
    ) {
        try {
            // 메일 링크 토큰 검증이 성공하면, 프론트에서 1회 확인 가능한 결과 토큰을 발급한다.
            String verifiedEmail = emailVerificationService.verifyByToken(token);
            String resultToken = emailVerificationService.issueVerificationResultToken(true, verifiedEmail);
            String verifiedRedirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("resultToken", resultToken)
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, verifiedRedirectUrl)
                    .build();
        } catch (UserEmailVerificationTokenInvalidException e) {
            // 검증 실패도 동일하게 결과 토큰을 발급해, 프론트가 직접 상태를 단정하지 못하게 한다.
            String resultToken = emailVerificationService.issueVerificationResultToken(false, null);
            String failureRedirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("resultToken", resultToken)
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, failureRedirectUrl)
                    .build();
        }
    }

    @PostMapping("/verify/result")
    public ResponseEntity<EmailVerifyResponse> verifyResult(
            @RequestBody @Validated EmailVerifyResultRequest request
    ) {
        // 결과 토큰은 1회 소모된다. 재사용이나 직접 URL 진입은 유효 토큰이 없으면 실패 처리된다.
        EmailVerificationService.VerificationResult result =
                emailVerificationService.consumeVerificationResultToken(request.getResultToken());
        return ResponseEntity.ok(new EmailVerifyResponse(result.verified(), result.email()));
    }
}
