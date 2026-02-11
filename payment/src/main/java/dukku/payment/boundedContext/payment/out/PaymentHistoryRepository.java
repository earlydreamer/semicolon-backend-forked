package dukku.payment.boundedContext.payment.out;

import dukku.payment.boundedContext.payment.entity.PaymentHistory;
import dukku.common.shared.payment.type.PaymentHistoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 결제 이력 Repository
 */
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer> {

    Optional<PaymentHistory> findByUuid(UUID uuid);

    List<PaymentHistory> findByPaymentId(int paymentId);

    List<PaymentHistory> findByType(PaymentHistoryType type);

    /**
     * 특정 결제의 특정 타입 이력 존재 여부 확인 (보상 트랜잭션 멱등성 체크용)
     */
    boolean existsByPaymentIdAndType(int paymentId, PaymentHistoryType type);
}
