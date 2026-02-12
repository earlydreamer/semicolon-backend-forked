package dukku.product.boundedContext.product.app.usecase.comment;

import dukku.product.boundedContext.product.app.cqrs.ProductStatsRedisSupport;
import dukku.product.boundedContext.product.entity.ProductComment;
import dukku.product.boundedContext.product.out.ProductCommentRepository;
import dukku.common.shared.product.exception.CommentNotFoundException;
import dukku.common.shared.product.exception.CommentUnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final ProductCommentRepository commentRepository;
    private final ProductStatsRedisSupport productStatsRedisSupport;

    @Transactional
    public void execute(UUID userUuid, UUID productUuid, UUID commentUuid) {

        ProductComment comment = commentRepository.findByUuid(commentUuid)
                .orElseThrow(CommentNotFoundException::new);

        int productId = comment.getProduct().getId();

        // 1) 상품 소속 검증 (404로 숨김)
        if (!comment.getProduct().getUuid().equals(productUuid)) {
            throw new CommentNotFoundException();
        }

        // 2) 작성자 검증
        if (!comment.getAuthorUuid().equals(userUuid)) {
            throw new CommentUnauthorizedException();
        }

        // 3) 소프트 삭제
        comment.softDelete();

        // 4) 상품 댓글수 감소
        productStatsRedisSupport.decrementComment(productId);
    }
}
