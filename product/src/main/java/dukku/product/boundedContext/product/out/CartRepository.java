package dukku.product.boundedContext.product.out;

import dukku.product.boundedContext.product.entity.Cart;
import dukku.product.boundedContext.product.entity.Product;
import dukku.product.boundedContext.product.entity.ProductUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    @Query("SELECT DISTINCT c FROM Cart c " +
            "JOIN FETCH c.product p " +
            "LEFT JOIN FETCH p.images " +
            "WHERE c.user.userUuid = :userUuid")
    List<Cart> findAllWithProductByUserUuid(@Param("userUuid") UUID userUuid);

    Optional<Cart> findByIdAndUser_UserUuid(int cartId, UUID userUuid);

    boolean existsByUserAndProduct(ProductUser user, Product product);

    void deleteByUser_UserUuid(UUID userUuid);
}