package dukku.product.boundedContext.product.in;

import dukku.common.shared.product.docs.CartApiDocs;
import dukku.common.shared.product.dto.cart.CartInternalResponse;
import dukku.common.shared.product.dto.cart.CartListResponse;
import dukku.product.boundedContext.product.app.facade.CartFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@CartApiDocs.CartTag
public class CartController {

    private final CartFacade cartFacade;

    @PostMapping("/{productUuid}")
    @CartApiDocs.CreateCart
    public ResponseEntity<Void> createCart(@PathVariable UUID productUuid) {
        cartFacade.createCart(productUuid);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 선택된 항목 삭제 (body 예: { "cartIds": [1,2,3] })
    @DeleteMapping
    public ResponseEntity<Void> deleteSelectedCartItems(@RequestBody CartIdsRequest request) {
        List<Integer> cartIds = request.cartIds();
        if (cartIds == null || cartIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        cartFacade.deleteCartItems(cartIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @CartApiDocs.FindMyCartList
    public ResponseEntity<CartListResponse> findMyCartList() {
        CartListResponse response = cartFacade.findMyCartList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @CartApiDocs.DeleteAllCartItem
    public ResponseEntity<Void> deleteAllCartItem() {
        cartFacade.deleteAllCartItem();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/{userUuid}")
    @CartApiDocs.FindCartListByUserUuid
    public ResponseEntity<CartInternalResponse> findCartListByUserUuid(@PathVariable UUID userUuid) {
        CartInternalResponse response = cartFacade.findCartListByUserUuid(userUuid);
        return ResponseEntity.ok(response);
    }

    private record CartIdsRequest(List<Integer> cartIds) {}
}
