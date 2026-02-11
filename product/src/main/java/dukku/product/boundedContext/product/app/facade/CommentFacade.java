package dukku.product.boundedContext.product.app.facade;

import dukku.product.boundedContext.product.app.usecase.comment.*;
import dukku.common.shared.product.dto.comment.CommentCreateRequest;
import dukku.common.shared.product.dto.comment.CommentListResponse;
import dukku.common.shared.product.dto.comment.CommentResponse;
import dukku.common.shared.product.dto.comment.CommentUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentFacade {
    private final CreateCommentUseCase createCommentUseCase;
    private final CreateCommentReplyUseCase createCommentReplyUseCase;
    private final FindCommentsListUseCase findCommentsListUseCase;
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;

    public CommentResponse createComment(UUID userUuid, UUID productUuid, @Valid CommentCreateRequest request) {
        return createCommentUseCase.execute(userUuid, productUuid, request);
    }

    public CommentResponse createReply(UUID userUuid, UUID productUuid, UUID parentCommentUuid, @Valid CommentCreateRequest request) {
        return createCommentReplyUseCase.execute(userUuid, productUuid, parentCommentUuid, request);
    }

    public CommentListResponse findCommentsList(UUID productUuid, Pageable pageable) {
        return findCommentsListUseCase.execute(productUuid, pageable);
    }

    public CommentResponse updateComment(UUID userUuid, UUID productUuid, UUID commentUuid, @Valid CommentUpdateRequest request) {
        return updateCommentUseCase.execute(userUuid, productUuid, commentUuid, request);
    }

    public void deleteComment(UUID userUuid, UUID productUuid, UUID commentUuid) {
        deleteCommentUseCase.execute(userUuid, productUuid, commentUuid);
    }
}
