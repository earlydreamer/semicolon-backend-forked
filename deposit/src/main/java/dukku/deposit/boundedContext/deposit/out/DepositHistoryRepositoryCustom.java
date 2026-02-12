package dukku.deposit.boundedContext.deposit.out;

import dukku.deposit.boundedContext.deposit.entity.DepositHistory;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface DepositHistoryRepositoryCustom {
    Slice<DepositHistory> findHistoriesByCursor(UUID userUuid, Integer cursor, int size);
}
