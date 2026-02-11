package dukku.deposit.boundedContext.deposit.app;

import dukku.deposit.boundedContext.deposit.entity.Deposit;
import dukku.common.shared.deposit.exception.DepositNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindDepositByDepositUuidUseCase {

    private final DepositSupport depositSupport;

    public Deposit execute(UUID depositUuid) {
        return depositSupport.findByDepositUuid(depositUuid)
                .orElseThrow(DepositNotFoundException::new);
    }
}
