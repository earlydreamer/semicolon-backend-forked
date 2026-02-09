package dukku.semicolon.boundedContext.product.out;

import dukku.semicolon.boundedContext.product.entity.SellerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SellerReviewRepository extends JpaRepository<SellerReview, Integer> {

    // 삭제된 리뷰가 있어도 재작성 허용
    boolean existsByOrderItemUuidAndDeletedAtIsNull(UUID orderItemUuid);

    Optional<SellerReview> findByUuid(UUID reviewUuid);

    // 목록은 삭제 포함 (마스킹 정책)
    Page<SellerReview> findBySellerUuid(UUID sellerUuid, Pageable pageable);

    // 수정/삭제 같은 write 쪽에서만 사용
    Optional<SellerReview> findByUuidAndDeletedAtIsNull(UUID reviewUuid);

    long countBySellerUuidAndDeletedAtIsNull(UUID sellerUuid);

    @Query("select coalesce(avg(r.rating), 0) " +
            "from SellerReview r " +
            "where r.sellerUuid = :sellerUuid and r.deletedAt is null")
    double avgRating(UUID sellerUuid);
}
