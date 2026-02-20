package dukku.deposit.boundedContext.deposit.app;

import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import dukku.deposit.boundedContext.deposit.entity.ProcessedRefundEvent;
import dukku.deposit.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.deposit.boundedContext.deposit.out.DepositRepository;
import dukku.deposit.boundedContext.deposit.out.ProcessedRefundEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Slice;

@Component
@RequiredArgsConstructor
public class DepositSupport {

    private final DepositRepository depositRepository;
    private final DepositHistoryRepository depositHistoryRepository;
    private final ProcessedRefundEventRepository processedRefundEventRepository;

    public Optional<Deposit> findByUserUuid(UUID userUuid) {
        return depositRepository.findByUserUuid(userUuid);
    }

    public Optional<Deposit> findByDepositUuid(UUID depositUuid) {
        return depositRepository.findByDepositUuid(depositUuid);
    }

    public Deposit save(Deposit deposit) {
        return depositRepository.save(deposit);
    }

    public void deleteAllByUserUuid(UUID userUuid) {
        depositHistoryRepository.deleteByUserUuid(userUuid);
        depositRepository.deleteById(userUuid);
    }

    public DepositHistory saveHistory(DepositHistory history) {
        return depositHistoryRepository.save(history);
    }

    public List<DepositHistory> findHistoriesByUserUuid(UUID userUuid) {
        return depositHistoryRepository.findByUserUuidOrderByCreatedAtDesc(userUuid);
    }

    public List<DepositHistory> findAllHistories() {
        return depositHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    public Slice<DepositHistory> findHistoriesByCursor(UUID userUuid, Integer cursor, int size) {
        return depositHistoryRepository.findHistoriesByCursor(userUuid, cursor, size);
    }

    /**
     * 특정 orderItemUuid(또는 settlementUuid)로 이미 처리된 이력이 있는지 확인 (멱등성 체크)
     */
    public boolean existsByOrderItemUuid(UUID orderItemUuid) {
        return depositHistoryRepository.existsByOrderItemUuid(orderItemUuid);
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean tryMarkRefundCompleted(UUID refundUuid, UUID orderUuid, Long refundAmount) {
        if (processedRefundEventRepository.existsByRefundUuid(refundUuid)) {
            return false;
        }
        processedRefundEventRepository.save(ProcessedRefundEvent.create(refundUuid, orderUuid, refundAmount));
        return true;
    }
}
