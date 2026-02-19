package dukku.payment.boundedContext.payment.out;

import dukku.payment.boundedContext.payment.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 환불 상품 Repository
 */
public interface RefundItemRepository extends JpaRepository<RefundItem, Integer> {

    Optional<RefundItem> findByUuid(UUID uuid);

    List<RefundItem> findByRefundId(int refundId);

    List<RefundItem> findByPaymentOrderItemId(int paymentOrderItemId);
}
