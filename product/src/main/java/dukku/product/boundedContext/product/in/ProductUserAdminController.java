package dukku.product.boundedContext.product.in;

import dukku.product.boundedContext.product.app.usecase.product.FindProductByUuidUseCase;
import dukku.product.boundedContext.product.app.usecase.product.AdminSuspendProductUseCase;
import dukku.product.boundedContext.product.app.usecase.product.AdminDeleteProductUseCase;
import dukku.product.boundedContext.product.entity.Product;
import dukku.common.shared.user.dto.UserAdminProfileResponse;
import dukku.common.shared.user.out.UserApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class ProductUserAdminController {

    private final FindProductByUuidUseCase findProductByUuidUseCase;
    private final AdminSuspendProductUseCase adminSuspendProductUseCase;
    private final AdminDeleteProductUseCase adminDeleteProductUseCase;
    private final UserApiClient userApiClient;

    @GetMapping("/{productUuid}/user")
    public ResponseEntity<UserAdminProfileResponse> getUserAdminProfile(@PathVariable UUID productUuid) {
        Product product = findProductByUuidUseCase.execute(productUuid);
        UserAdminProfileResponse response = userApiClient.getUserAdminProfile(product.getSellerUuid());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productUuid}/suspend")
    public ResponseEntity<Void> suspendProduct(@PathVariable UUID productUuid) {
        adminSuspendProductUseCase.execute(productUuid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productUuid}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productUuid) {
        adminDeleteProductUseCase.execute(productUuid);
        return ResponseEntity.noContent().build();
    }
}
