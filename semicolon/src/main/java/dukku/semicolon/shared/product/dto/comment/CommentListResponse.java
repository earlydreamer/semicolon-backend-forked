package dukku.semicolon.shared.product.dto.comment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentListResponse {
    private java.util.List<CommentThreadResponse> items;
    private int page;
    private int size;
    private long totalCount;
    private boolean hasNext;

    @Getter
    @Builder
    public static class CommentThreadResponse {
        private CommentResponse parent;
        private java.util.List<CommentResponse> replies;
    }
}
