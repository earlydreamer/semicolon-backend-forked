package dukku.semicolon.boundedContext.user.in;

import dukku.semicolon.boundedContext.user.app.email.EmailVerificationService;
import dukku.semicolon.boundedContext.user.in.dto.EmailSendRequest;
import dukku.semicolon.boundedContext.user.in.dto.EmailVerifyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    @PostMapping("/send")
    public ResponseEntity<Void> sendVerificationCode(
            @RequestBody @Validated EmailSendRequest request
    ) {
        emailVerificationService.sendVerificationLink(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<EmailVerifyResponse> verify(
            @RequestParam("token") String token
    ) {
        String email = emailVerificationService.verifyByToken(token);
        return ResponseEntity.ok(new EmailVerifyResponse(true, email));
    }
}
