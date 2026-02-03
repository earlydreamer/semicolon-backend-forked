package dukku.semicolon.shared.product.dto.comment;

import dukku.semicolon.boundedContext.product.entity.Product;
import dukku.semicolon.boundedContext.product.entity.ProductComment;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CommentResponse {
    private UUID commentUuid;
    private UUID productUuid;
    private UUID authorUuid;
    private String authorRole; // "SELLER" | "BUYER"
    private String content;
    private UUID parentCommentUuid; // null이면 부모댓글

    public static CommentResponse from(ProductComment comment) {
        Product product = comment.getProduct();

        String role = product.getSellerUuid().equals(comment.getAuthorUuid())
                ? "SELLER"
                : "BUYER";

        String content = comment.isDeleted()
                ? "삭제된 댓글입니다."
                : comment.getContent();

        return CommentResponse.builder()
                .commentUuid(comment.getUuid())
                .productUuid(product.getUuid())
                .authorUuid(comment.getAuthorUuid())
                .authorRole(role)
                .content(content)
                .parentCommentUuid(comment.getParent() == null ? null : comment.getParent().getUuid())
                .build();
    }
}
