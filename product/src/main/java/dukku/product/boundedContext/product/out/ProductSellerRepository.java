package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.ProductSeller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductSellerRepository extends JpaRepository<ProductSeller, Integer>, CustomProductSellerRepository {
    Optional<ProductSeller> findByUserUuid(UUID userUuid);

    Optional<ProductSeller> findByUuid(UUID shopUuid);
}
