package dukku.user.boundedContext.user.in;

import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.in.dto.EmailSendRequest;
import dukku.common.shared.user.exception.UserEmailVerificationTokenInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/users/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Value("${custom.email.verification.success-redirect-url:https://dukku.shop}")
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
            emailVerificationService.verifyByToken(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, successRedirectUrl)
                    .build();
        } catch (UserEmailVerificationTokenInvalidException e) {
            String failureRedirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("error", "이메일_인증_실패")
                    .build()
                    .toUriString();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, failureRedirectUrl)
                    .build();
        }
    }
}
