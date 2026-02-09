package dukku.semicolon.boundedContext.product.app.usecase.product;

import dukku.semicolon.boundedContext.product.app.support.ProductSupport;
import dukku.semicolon.boundedContext.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FindProductByUuidUseCase {

    private final ProductSupport productSupport;

    @Transactional(readOnly = true)
    public Product execute(UUID productUuid) {
        return productSupport.findByUuid(productUuid);
    }
}
