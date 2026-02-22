package dukku.settlement.boundedContext.settlement.out;

import dukku.common.shared.settlement.type.SettlementStatus;
import dukku.settlement.boundedContext.settlement.entity.Settlement;
import dukku.settlement.boundedContext.settlement.out.impl.SettlementRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, SettlementRepositoryCustom {

    Optional<Settlement> findByUuid(UUID settlementUuid);

    long countByOrderIdAndSettlementStatusNot(UUID orderId, SettlementStatus status);

    List<Settlement> findBySellerUuidAndCreatedAtAfter(UUID sellerUuid, LocalDateTime after);

}
