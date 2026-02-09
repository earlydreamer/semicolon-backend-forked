package dukku.semicolon.boundedContext.deposit.app;

import dukku.semicolon.boundedContext.deposit.entity.Deposit;
import dukku.semicolon.shared.deposit.exception.DepositNotFoundException;
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
