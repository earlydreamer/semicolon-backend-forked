package dukku.semicolon.boundedContext.product.out;

import dukku.semicolon.boundedContext.product.entity.ProductSeller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductSellerRepository extends JpaRepository<ProductSeller, Integer> {
    Optional<ProductSeller> findByUserUuid(UUID userUuid);

    Optional<ProductSeller> findByUuid(UUID shopUuid);

    Optional<ProductSeller> findBySellerUuid(UUID sellerUuid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ProductSeller ps
           set ps.reviewCount = :reviewCount,
               ps.averageRating = :averageRating
         where ps.sellerUuid = :sellerUuid
    """)
    int updateReviewSummary(UUID sellerUuid, int reviewCount, BigDecimal averageRating);
}
