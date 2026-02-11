package dukku.product.boundedContext.product.app.usecase.comment;

import dukku.common.shared.product.dto.comment.CommentCreateRequest;
import dukku.common.shared.product.dto.comment.CommentResponse;
import dukku.common.shared.product.exception.ProductNotFoundException;
import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductComment;
import dukku.product.boundedContext.product.out.ProductCommentRepository;
import dukku.product.boundedContext.product.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateCommentUseCase {

    private final ProductRepository productRepository;
    private final ProductCommentRepository commentRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    @Transactional
    public CommentResponse execute(UUID authorUuid, UUID productUuid, CommentCreateRequest request) {

        Product product = productRepository.findByUuidAndDeletedAtIsNull(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        ProductComment saved = commentRepository.save(
                ProductComment.createRoot(product, authorUuid, request.getContent())
        );

        productStatsRedisSupport.incrementComment(saved.getProduct().getId());

        return ProductComment.from(saved);
    }
}
