package dukku.user.boundedContext.user.in;

import dukku.common.shared.user.exception.UserEmailVerificationTokenInvalidException;
import dukku.user.boundedContext.user.app.email.EmailVerificationService;
import dukku.user.boundedContext.user.in.dto.EmailSendRequest;
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
            String verifiedEmail = emailVerificationService.verifyByToken(token);
            String verifiedRedirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("verified", true)
                    .queryParam("email", verifiedEmail)
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, verifiedRedirectUrl)
                    .build();
        } catch (UserEmailVerificationTokenInvalidException e) {
            String failureRedirectUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                    .queryParam("verified", false)
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, failureRedirectUrl)
                    .build();
        }
    }
}
