package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Integer> {

    Optional<ProductComment> findByUuid(UUID uuid);

    Optional<ProductComment> findByUuidAndProduct_Uuid(UUID commentUuid, UUID productUuid);

    // 부모댓글(삭제 포함) 페이징
    @EntityGraph(attributePaths = {"product", "parent"})
    Page<ProductComment> findByProduct_UuidAndParentIsNull(
            UUID productUuid, Pageable pageable
    );

    // 대댓글(삭제 포함) 한방 조회
    @EntityGraph(attributePaths = {"product", "parent"})
    List<ProductComment> findByProduct_UuidAndParent_UuidIn(
            UUID productUuid, Collection<UUID> parentUuids
    );
}
