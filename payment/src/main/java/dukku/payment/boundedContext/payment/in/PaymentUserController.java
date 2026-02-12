package dukku.payment.boundedContext.payment.in;

import dukku.payment.boundedContext.payment.app.FindPaymentUseCase;
import dukku.payment.boundedContext.payment.entity.Payment;
import dukku.common.shared.user.dto.UserProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentUserController {

    private final FindPaymentUseCase findPaymentUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{paymentUuid}/user")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID paymentUuid) {
        Payment payment = findPaymentUseCase.execute(paymentUuid);
        UserProfileResponse response = userApiClient.getUserProfile(payment.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
