package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductPayload;
import dukku.common.shared.product.dto.product.ProductUpdateRequest;
import dukku.common.shared.product.event.ProductSyncEvent;
import dukku.common.shared.product.exception.ProductCategoryNotFoundException;
import dukku.common.shared.product.type.ProductEventType;
import dukku.product.boundedContext.product.app.support.ProductMapper;
import dukku.product.boundedContext.product.app.support.ProductSupport;
import dukku.product.boundedContext.product.app.support.ProductTagSupport;
import dukku.product.boundedContext.product.entity.Category;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.tag.Tag;
import dukku.product.boundedContext.product.out.CategoryRepository;
import dukku.product.global.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static dukku.product.boundedContext.product.entity.Product.toProductPayload;

@Component
@RequiredArgsConstructor
public class UpdateProductUseCase {
    private static final ProductEventType PRODUCT_EVENT_TYPE = ProductEventType.UPDATED;

    private final ProductSupport productSupport;
    private final ProductTagSupport productTagSupport;
    private final CategoryRepository categoryRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ProductDetailResponse execute(UUID productUuid, UUID sellerUuid, ProductUpdateRequest request) {
        Product product = productSupport.getProduct(productUuid, sellerUuid);

        boolean isCategoryChanged = false;
        Category category = null;

        if (!product.getCategory().getId().equals(request.categoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) {
                throw new ProductCategoryNotFoundException();
            }
            category = categoryRepository.getReferenceById(request.categoryId());
            isCategoryChanged = true;
        }

        product.update(
                category,
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

        if (request.tags() != null) {
            List<Tag> tags = productTagSupport.getOrCreateTags(request.tags());
            product.replaceTags(tags);
        }

        eventPublisher.publish(new ProductUpdatedEvent(product.getId(), isCategoryChanged));

        ProductPayload payload = toProductPayload(product, PRODUCT_EVENT_TYPE);
        eventPublisher.publish(new ProductSyncEvent(payload));

        return ProductMapper.toDetail(product);
    }
}