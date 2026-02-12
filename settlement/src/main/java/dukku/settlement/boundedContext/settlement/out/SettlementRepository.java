package dukku.settlement.boundedContext.settlement.out;

import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.settlement.boundedContext.settlement.out.impl.SettlementRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, SettlementRepositoryCustom {

    Optional<Settlement> findByUuid(UUID settlementUuid);

}
