package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductUpdateRequest;
import dukku.common.shared.product.exception.ProductCategoryNotFoundException;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.app.support.ProductSupport;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.global.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 판매자가 바꿀 경우
@Component
@RequiredArgsConstructor
public class UpdateProductUseCase {
    private final ProductSupport productSupport;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductDetailResponse execute(UUID productUuid, UUID sellerUuid, ProductUpdateRequest request) {
        Product product = productSupport.getProduct(productUuid, sellerUuid);

        // 1. 카테고리 변경 감지 로직
        boolean isCategoryChanged = false;
        Category category = null;

        // 요청한 카테고리가 현재와 다를 때만 DB 조회
        if (!product.getCategory().getId().equals(request.categoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) {
                throw new ProductCategoryNotFoundException();
            }
            category = categoryRepository.getReferenceById(request.categoryId());
            isCategoryChanged = true;
        }

        // 2. 엔티티 업데이트 (category가 null이면 기존 유지되도록 Product.update 내부 로직 활용)
        product.update(
                category, // 변경 없으면 null 넘어감
                request.title(),
                request.description(),
                request.price(),
                request.shippingFee(),
                request.conditionStatus(),
                request.visibilityStatus()
        );

        if (request.imageUrls() != null) {
            productSupport.validateImageCount(request.imageUrls().size());
            product.replaceImages(request.imageUrls());
        }

        // 3. 이벤트 발행 (변경 플래그 포함)
        eventPublisher.publishEvent(new ProductUpdatedEvent(product, isCategoryChanged));

        return ProductMapper.toDetail(product);
    }
}