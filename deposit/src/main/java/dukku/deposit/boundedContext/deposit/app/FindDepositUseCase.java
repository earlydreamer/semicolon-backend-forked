package dukku.deposit.boundedContext.deposit.app;

import dukku.deposit.boundedContext.deposit.entity.Deposit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 예치금 조회 UseCase (도메인 서비스)
 *
 * <p>
 * 사용자 예치금을 조회하고, 없으면 생성하는 역할을 담당
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindDepositUseCase {

    private final DepositSupport depositSupport;

    /**
     * 예치금 조회 및 생성
     *
     * <p>
     * 사용자 예치금 정보를 조회한다. 존재하지 않을 경우 새로 생성하여 반환한다.
     */
    @Transactional
    public Deposit findOrCreate(UUID userUuid) {
        return depositSupport.findByUserUuid(userUuid)
                .orElseGet(() -> {
                    Deposit newDeposit = Deposit.create(userUuid);
                    return depositSupport.save(newDeposit);
                });
    }

    /**
     * 예치금 조회/생성 + 쓰기 잠금 획득.
     */
    @Transactional
    public Deposit findOrCreateForUpdate(UUID userUuid) {
        return depositSupport.findByUserUuidForUpdate(userUuid)
                .orElseGet(() -> createAndRefetchWithLock(userUuid));
    }

    private Deposit createAndRefetchWithLock(UUID userUuid) {
        try {
            return depositSupport.save(Deposit.create(userUuid));
        } catch (DataIntegrityViolationException ex) {
            return depositSupport.findByUserUuidForUpdate(userUuid)
                    .orElseThrow(() -> ex);
        }
    }
}
