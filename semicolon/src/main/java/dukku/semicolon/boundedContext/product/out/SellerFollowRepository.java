package dukku.semicolon.boundedContext.product.out;

import dukku.semicolon.boundedContext.product.entity.SellerFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SellerFollowRepository extends JpaRepository<SellerFollow, Integer> {

    boolean existsByUserUuidAndSellerUuid(UUID userUuid, UUID sellerUuid);

    void deleteByUserUuidAndSellerUuid(UUID userUuid, UUID sellerUuid);

    List<SellerFollow> findByUserUuid(UUID userUuid);

    List<SellerFollow> findBySellerUuid(UUID sellerUuid);

    long countBySellerUuid(UUID sellerUuid);
}
