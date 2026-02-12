package dukku.product.boundedContext.product.out;

import dukku.common.shared.product.type.SaleStatus;
import dukku.common.shared.product.type.VisibilityStatus;
import dukku.product.boundedContext.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Integer>, CustomProductRepository {
    Optional<Integer> findIdByUuidAndDeletedAtIsNull(UUID productUuid);

    @Query("""
select distinct p
from Product p
left join fetch p.images i
left join fetch p.category c
where p.uuid = :uuid
""")
    Optional<Product> findByUuidWithImagesAndCategory(@Param("uuid") UUID uuid);

    Optional<Product> findByUuid(UUID productUuid);

    Optional<Product> findByUuidAndDeletedAtIsNull(UUID productUuid);

    List<Product> findAllByUuidIn(List<UUID> uuids);

    @EntityGraph(attributePaths = "images")
    List<Product> findByUuidInAndDeletedAtIsNull(List<UUID> uuids);

    @EntityGraph(attributePaths = "images")
    Page<Product> findBySellerUuidAndDeletedAtIsNull(UUID sellerUuid, Pageable pageable);

    @EntityGraph(attributePaths = "images")
    Page<Product> findBySellerUuidAndSaleStatusAndDeletedAtIsNull(
            UUID sellerUuid, SaleStatus saleStatus, Pageable pageable
    );

    // 1. 카테고리별 조회 (Fallback용) - 인덱스 필수! (category_id)
    Page<Product> findByCategory_IdInAndVisibilityStatusAndDeletedAtIsNull(
            List<Integer> categoryIds,
            VisibilityStatus visibilityStatus,
            Pageable pageable
    );

    // 2. 전체 최신순 조회 (Fallback용)
    // 검색어가 들어와도 그냥 이걸로 돌려버립니다.
    Page<Product> findByVisibilityStatusAndDeletedAtIsNull(
            VisibilityStatus visibilityStatus,
            Pageable pageable
    );
}
