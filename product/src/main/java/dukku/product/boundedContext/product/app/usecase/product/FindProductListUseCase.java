package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.shared.product.dto.product.ProductListResponse;
import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindProductListUseCase {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductListResponse execute(Integer categoryId, Pageable pageable) {
        Page<Product> result;
        if (categoryId == null) {
            result = productRepository.findByVisibilityStatusAndDeletedAtIsNull(VisibilityStatus.VISIBLE, pageable);
        } else {
            // ES와 동일하게 "선택 카테고리 + 하위 카테고리" 범위로 조회한다.
            List<Integer> categoryIds = categoryRepository.findCategoryTreeIds(categoryId);
            result = productRepository.findByCategory_IdInAndVisibilityStatusAndDeletedAtIsNull(categoryIds, VisibilityStatus.VISIBLE, pageable);
        }

        return Product.from(result);
    }
}
