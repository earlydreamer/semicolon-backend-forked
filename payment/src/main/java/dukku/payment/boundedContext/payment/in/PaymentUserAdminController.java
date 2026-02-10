package dukku.semicolon.boundedContext.payment.in;

import dukku.semicolon.boundedContext.payment.app.FindPaymentUseCase;
import dukku.semicolon.boundedContext.payment.entity.Payment;
import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentUserAdminController {

    private final FindPaymentUseCase findPaymentUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{paymentUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID paymentUuid) {
        Payment payment = findPaymentUseCase.execute(paymentUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(payment.getUserUuid());
        return ResponseEntity.ok(response);
    }
}
