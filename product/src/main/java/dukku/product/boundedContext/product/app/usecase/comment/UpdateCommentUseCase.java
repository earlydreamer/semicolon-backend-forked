package dukku.product.boundedContext.product.app.usecase.comment;

import dukku.common.shared.product.dto.comment.CommentResponse;
import dukku.common.shared.product.dto.comment.CommentUpdateRequest;
import dukku.common.shared.product.exception.CommentNotFoundException;
import dukku.common.shared.product.exception.CommentUnauthorizedException;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductComment;
import dukku.product.boundedContext.product.out.ProductCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateCommentUseCase {

    private final ProductCommentRepository commentRepository;

    @Transactional
    public CommentResponse execute(UUID userUuid, UUID productUuid, UUID commentUuid, CommentUpdateRequest request) {

        ProductComment comment = commentRepository.findByUuid(commentUuid)
                .orElseThrow(CommentNotFoundException::new);

        Product product = comment.getProduct();

        if (comment.isDeleted()) throw new CommentUnauthorizedException();

        // (1) 상품 소속 검증
        if (!product.getUuid().equals(productUuid)) {
            throw new CommentNotFoundException();
        }

        // (2) 작성자 검증
        if (!comment.getAuthorUuid().equals(userUuid)) {
            throw new CommentUnauthorizedException();
        }

        // (3) 수정
        comment.changeContent(request.getContent());

        return ProductComment.from(comment);
    }
}
