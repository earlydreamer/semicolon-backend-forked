package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.ProductUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductUserRepository extends JpaRepository<ProductUser, UUID> {
}
