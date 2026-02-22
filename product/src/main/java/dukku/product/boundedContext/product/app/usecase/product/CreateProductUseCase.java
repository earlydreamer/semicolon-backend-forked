package dukku.product.boundedContext.product.app.usecase.product;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.product.dto.product.ProductCreateRequest;
import dukku.common.shared.product.dto.product.ProductDetailResponse;
import dukku.common.shared.product.dto.product.ProductPayload;
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
import dukku.product.boundedContext.product.out.ProductRepository;
import dukku.product.global.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static dukku.product.boundedContext.product.entity.Product.toProductPayload;

@Component
@RequiredArgsConstructor
public class CreateProductUseCase {
    private static final ProductEventType PRODUCT_EVENT_TYPE = ProductEventType.CREATED;

    private final ProductSupport productSupport;
    private final ProductTagSupport productTagSupport;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    public ProductDetailResponse execute(UUID sellerUuid, ProductCreateRequest request) {
        if (!productSupport.existsByCategoryId(request.getCategoryId())) {
            throw new ProductCategoryNotFoundException();
        }

        Category category = categoryRepository.getReferenceById(request.getCategoryId());
        Product product = Product.create(
                sellerUuid,
                category,
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getShippingFee(),
                request.getConditionStatus()
        );

        List<String> imageUrls = request.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            productSupport.validateImageCount(imageUrls.size());
            imageUrls.forEach(product::addImage);
        }

        List<Tag> tags = productTagSupport.getOrCreateTags(request.getTags());
        product.replaceTags(tags);

        Product savedProduct = productRepository.save(product);
        eventPublisher.publish(new ProductCreatedEvent(savedProduct.getId()));

        ProductPayload payload = toProductPayload(savedProduct, PRODUCT_EVENT_TYPE);
        eventPublisher.publish(new ProductSyncEvent(payload));

        return ProductMapper.toDetail(savedProduct);
    }
}
