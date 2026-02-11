package dukku.product.boundedContext.product.in;

import dukku.common.global.UserUtil;
import dukku.product.boundedContext.product.app.facade.CommentFacade;
import dukku.common.shared.product.docs.CommentApiDocs;
import dukku.common.shared.product.dto.comment.CommentCreateRequest;
import dukku.common.shared.product.dto.comment.CommentListResponse;
import dukku.common.shared.product.dto.comment.CommentResponse;
import dukku.common.shared.product.dto.comment.CommentUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@CommentApiDocs.CommentTag
public class CommentController {

    private final CommentFacade commentFacade;

    // 댓글 작성 (부모 댓글)
    @PostMapping("/{productUuid}/comments")
    @CommentApiDocs.CreateComment
    public CommentResponse createComment(
            @PathVariable UUID productUuid,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        return commentFacade.createComment(UserUtil.getUserId(), productUuid, request);
    }

    // 대댓글 작성 (부모댓글 UUID 필요)
    @PostMapping("/{productUuid}/comments/{parentCommentUuid}/replies")
    @CommentApiDocs.CreateReply
    public CommentResponse createReply(
            @PathVariable UUID productUuid,
            @PathVariable UUID parentCommentUuid,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        return commentFacade.createReply(UserUtil.getUserId(), productUuid, parentCommentUuid, request);
    }

    // 상품 댓글 목록 조회 (부모 + 대댓글 포함)
    @GetMapping("/{productUuid}/comments")
    @CommentApiDocs.FindCommentsList
    public CommentListResponse findCommentsList(
            @PathVariable UUID productUuid,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return commentFacade.findCommentsList(productUuid, PageRequest.of(page, size));
    }

    // 댓글 수정
    @PatchMapping("/{productUuid}/comments/{commentUuid}")
    @CommentApiDocs.UpdateComment
    public CommentResponse updateComment(
            @PathVariable UUID productUuid,
            @PathVariable UUID commentUuid,
            @RequestBody @Valid CommentUpdateRequest request
    ) {
        return commentFacade.updateComment(UserUtil.getUserId(), productUuid, commentUuid, request);
    }

    // 댓글 삭제(소프트 삭제)
    @DeleteMapping("/{productUuid}/comments/{commentUuid}")
    @CommentApiDocs.DeleteComment
    public void deleteComment(
            @PathVariable UUID productUuid,
            @PathVariable UUID commentUuid
    ) {
        commentFacade.deleteComment(UserUtil.getUserId(), productUuid, commentUuid);
    }
}
