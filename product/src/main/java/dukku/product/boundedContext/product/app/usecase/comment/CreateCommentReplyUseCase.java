package dukku.product.boundedContext.product.app.usecase.comment;

import dukku.common.shared.product.dto.comment.CommentCreateRequest;
import dukku.common.shared.product.dto.comment.CommentResponse;
import dukku.common.shared.product.exception.CommentNotFoundException;
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
public class CreateCommentReplyUseCase {

    private final ProductRepository productRepository;
    private final ProductCommentRepository commentRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    @Transactional
    public CommentResponse execute(UUID authorUuid, UUID productUuid, UUID parentCommentUuid, CommentCreateRequest request) {

        // 1) 상품 존재 확인
        Product product = productRepository.findByUuidAndDeletedAtIsNull(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        // 2) 부모 댓글 존재 확인
        ProductComment parent = commentRepository.findByUuidAndProduct_Uuid(productUuid, parentCommentUuid)
                .orElseThrow(CommentNotFoundException::new);

        // 3) 부모 댓글이 삭제된 상태인지 확인
        if (parent.isDeleted()) {
            throw new CommentNotFoundException(); // 숨김
        }

        ProductComment saved = commentRepository.save(
                ProductComment.createReply(product, authorUuid, parent, request.getContent())
        );

        productStatsRedisSupport.incrementComment(saved.getProduct().getId());

        return ProductComment.from(saved);
    }
}
