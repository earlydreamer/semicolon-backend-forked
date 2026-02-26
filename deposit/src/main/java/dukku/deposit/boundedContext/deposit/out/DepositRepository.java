package dukku.deposit.boundedContext.deposit.out;

import dukku.deposit.boundedContext.deposit.entity.Deposit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * 예치금 Repository
 */
public interface DepositRepository extends JpaRepository<Deposit, UUID> {

    Optional<Deposit> findByUserUuid(UUID userUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deposit d where d.userUuid = :userUuid")
    Optional<Deposit> findByUserUuidForUpdate(@Param("userUuid") UUID userUuid);

    Optional<Deposit> findByDepositUuid(UUID depositUuid);
}
