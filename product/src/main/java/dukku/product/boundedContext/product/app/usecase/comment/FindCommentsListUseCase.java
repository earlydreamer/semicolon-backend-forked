package dukku.product.boundedContext.product.app.usecase.comment;

import dukku.product.boundedContext.product.entity.ProductComment;
import dukku.product.boundedContext.product.out.ProductCommentRepository;
import dukku.common.shared.product.dto.comment.CommentListResponse;
import dukku.common.shared.product.dto.comment.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FindCommentsListUseCase {

    private final ProductCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public CommentListResponse execute(UUID productUuid, Pageable pageable) {

        Pageable p = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // 1) 부모댓글 페이징 조회 (parent is null)
        Page<ProductComment> parentPage = commentRepository
                .findByProduct_UuidAndParentIsNull(productUuid, p);

        List<ProductComment> parents = parentPage.getContent();

        // 부모 없으면 바로 반환
        if (parents.isEmpty()) {
            return CommentListResponse.builder()
                    .items(List.of())
                    .page(parentPage.getNumber())
                    .size(parentPage.getSize())
                    .totalCount(parentPage.getTotalElements())
                    .hasNext(parentPage.hasNext())
                    .build();
        }

        // 2) 부모 UUID 목록 추출
        List<UUID> parentUuids = parents.stream()
                .map(ProductComment::getUuid)
                .toList();

        // 3) replies를 한 번에 조회
        List<ProductComment> replies = commentRepository
                .findByProduct_UuidAndParent_UuidIn(productUuid, parentUuids);

        // 4) parentUuid 기준으로 grouping
        Map<UUID, List<ProductComment>> repliesByParentUuid = replies.stream()
                .collect(Collectors.groupingBy(r -> r.getParent().getUuid()));

        // 5) 응답 조립 (부모 댓글 순서를 유지하면서 replies 붙이기)
        List<CommentListResponse.CommentThreadResponse> threads = parents.stream()
                .map(parent -> {
                    List<ProductComment> rs = repliesByParentUuid.getOrDefault(parent.getUuid(), List.of());

                    // 대댓글 정렬: 오래된 순
                    List<CommentResponse> replyResponses = rs.stream()
                            .sorted(Comparator.comparing(ProductComment::getCreatedAt))
                            .map(CommentResponse::from)
                            .toList();

                    return CommentListResponse.CommentThreadResponse.builder()
                            .parent(CommentResponse.from(parent))
                            .replies(replyResponses)
                            .build();
                })
                .toList();

        return CommentListResponse.builder()
                .items(threads)
                .page(parentPage.getNumber())
                .size(parentPage.getSize())
                .totalCount(parentPage.getTotalElements())
                .hasNext(parentPage.hasNext())
                .build();
    }
}
