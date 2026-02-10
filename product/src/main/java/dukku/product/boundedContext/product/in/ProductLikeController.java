package dukku.semicolon.boundedContext.product.in;

import dukku.semicolon.boundedContext.product.app.facade.ProductLikeFacade;
import dukku.common.shared.product.docs.ProductLikeApiDocs;
import dukku.common.shared.product.dto.like.MyLikedProductListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@ProductLikeApiDocs.ProductLikeTag
public class ProductLikeController {

    private final ProductLikeFacade productLikeFacade;

    @PostMapping("/{productUuid}/like")
    @ProductLikeApiDocs.LikeProduct
    public ResponseEntity<Void> like(@PathVariable UUID productUuid) {
        productLikeFacade.like(productUuid);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productUuid}/like")
    @ProductLikeApiDocs.UnlikeProduct
    public ResponseEntity<Void> unlike(
            @PathVariable UUID productUuid
    ) {
        productLikeFacade.unlike(productUuid);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/likes/me")
    @ProductLikeApiDocs.MyLikes
    public MyLikedProductListResponse myLikes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productLikeFacade.myLikes(page, size);
    }
}
