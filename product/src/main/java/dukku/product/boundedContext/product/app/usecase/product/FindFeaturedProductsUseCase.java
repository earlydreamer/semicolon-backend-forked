package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.common.shared.product.dto.product.ProductListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FindFeaturedProductsUseCase {

    private final ProductRepository productRepository;

    public List<ProductListItemResponse> execute(int size) {
        Pageable pageable = PageRequest.of(
                0,
                Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "likeCount")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return productRepository.findByVisibilityStatusAndDeletedAtIsNull(VisibilityStatus.VISIBLE, pageable)
                .getContent().stream()
                .map(ProductMapper::toListItem)
                .toList();
    }
}
