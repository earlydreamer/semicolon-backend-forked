package dukku.semicolon.boundedContext.product.in;

import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductReserveRequest;
import dukku.semicolon.boundedContext.product.app.facade.ProductFacade;
import dukku.common.shared.product.docs.ProductApiDocs;
import dukku.common.shared.product.dto.cqrs.ProductSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@ProductApiDocs.ProductTag
public class ProductController {
    private final ProductFacade productFacade;

    @GetMapping("/categories")
    @ProductApiDocs.FindCategories
    public List<dukku.common.shared.product.dto.product.CategoryCreateResponse> findCategories() {
        return productFacade.findCategories();
    }

    @GetMapping("/products/featured")
    @ProductApiDocs.FindFeaturedProducts
    public List<dukku.common.shared.product.dto.product.ProductListItemResponse> findFeaturedProducts(
            @RequestParam(defaultValue = "20") int size
    ) {
        return productFacade.findFeatured(size);
    }

    @GetMapping("/products")
    @ProductApiDocs.FindProductList
    public dukku.common.shared.product.dto.product.ProductListResponse findProducts(ProductSearchRequest request,
                                                                                    @RequestParam int page,
                                                                                    @RequestParam int size) {
        return productFacade.findProducts(request, page, size);
    }

    @GetMapping("/products/{productUuid}")
    @ProductApiDocs.FindProductDetail
    public ProductDetailResponse findProductDetail(
            @PathVariable UUID productUuid
    ) {
        return productFacade.findProductDetail(productUuid);
    }

    @PostMapping("/products/internal/reserve")
    @ProductApiDocs.ReserveProducts
    public void reserveProducts(@RequestBody ProductReserveRequest request) {
        productFacade.reserveProducts(request);
    }
}