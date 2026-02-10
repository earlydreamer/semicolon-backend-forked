package dukku.semicolon.boundedContext.product.in;

import dukku.semicolon.boundedContext.product.app.usecase.product.FindProductByUuidUseCase;
import dukku.semicolon.boundedContext.product.entity.Product;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductUserController {

    private final FindProductByUuidUseCase findProductByUuidUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{productUuid}/user")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID productUuid) {
        Product product = findProductByUuidUseCase.execute(productUuid);
        UserProfileResponse response = userApiClient.getUserProfile(product.getSellerUuid());
        return ResponseEntity.ok(response);
    }
}
