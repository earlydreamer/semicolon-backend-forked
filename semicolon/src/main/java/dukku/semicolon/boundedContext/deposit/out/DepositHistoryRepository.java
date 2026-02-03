package dukku.semicolon.boundedContext.deposit.out;

import dukku.semicolon.boundedContext.deposit.entity.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 예치금 이력 Repository
 */
public interface DepositHistoryRepository
        extends JpaRepository<DepositHistory, Integer>, DepositHistoryRepositoryCustom {

    List<DepositHistory> findByUserUuidOrderByCreatedAtDesc(UUID userUuid);

    List<DepositHistory> findAllByOrderByCreatedAtDesc();

    List<DepositHistory> findByOrderItemUuid(UUID orderItemUuid);

    /**
     * 특정 orderItemUuid(또는 settlementUuid)로 이미 처리된 이력이 있는지 확인 (멱등성 체크)
     */
    boolean existsByOrderItemUuid(UUID orderItemUuid);
}
