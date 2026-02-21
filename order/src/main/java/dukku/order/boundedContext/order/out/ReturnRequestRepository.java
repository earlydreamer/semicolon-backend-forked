package dukku.order.boundedContext.order.out;

import dukku.order.boundedContext.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByUuid(UUID uuid);

    List<ReturnRequest> findByOrderUuid(UUID orderUuid);
}
