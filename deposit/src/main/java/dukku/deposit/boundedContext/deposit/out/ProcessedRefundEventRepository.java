package dukku.deposit.boundedContext.deposit.out;

import dukku.deposit.boundedContext.deposit.entity.ProcessedRefundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 환불 이벤트 처리 여부를 조회해 중복 반영을 차단하는 레포지토리
 */
public interface ProcessedRefundEventRepository extends JpaRepository<ProcessedRefundEvent, Integer> {
    boolean existsByRefundUuid(UUID refundUuid);
}
