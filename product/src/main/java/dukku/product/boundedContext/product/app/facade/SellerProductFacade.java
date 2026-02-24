package dukku.product.boundedContext.product.app.facade;

import dukku.common.global.UserUtil;
import dukku.product.boundedContext.product.app.usecase.product.CreateProductUseCase;
import dukku.product.boundedContext.product.app.usecase.product.DeleteProductUseCase;
import dukku.product.boundedContext.product.app.usecase.product.UpdateProductUseCase;
import dukku.common.shared.product.dto.product.ProductCreateRequest;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerProductFacade {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;

    public ProductDetailResponse create(ProductCreateRequest request) {
        return createProductUseCase.execute(UserUtil.getUserId(), request);
    }

    public ProductDetailResponse update(UUID productUuid, ProductUpdateRequest request) {
        return updateProductUseCase.execute(productUuid, UserUtil.getUserId(), request);
    }

    public void delete(UUID productUuid) {
        deleteProductUseCase.execute(productUuid, UserUtil.getUserId());
    }
}
