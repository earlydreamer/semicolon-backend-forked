package dukku.semicolon.boundedContext.deposit.app;

import dukku.semicolon.boundedContext.deposit.entity.Deposit;
import dukku.semicolon.boundedContext.deposit.entity.DepositHistory;
import dukku.semicolon.boundedContext.deposit.out.DepositHistoryRepository;
import dukku.semicolon.boundedContext.deposit.out.DepositRepository;
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

    public Optional<Deposit> findByUserUuid(UUID userUuid) {
        return depositRepository.findByUserUuid(userUuid);
    }

    public Optional<Deposit> findByDepositUuid(UUID depositUuid) {
        return depositRepository.findByDepositUuid(depositUuid);
    }

    public Deposit save(Deposit deposit) {
        return depositRepository.save(deposit);
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
}
