package dukku.logconsumer.repository;

import dukku.logconsumer.document.AnomalyLogDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnomalyLogRepository extends MongoRepository<AnomalyLogDoc, String> {

    List<AnomalyLogDoc> findBySettlementUuidOrderByDetectedAtDesc(String settlementUuid);

    List<AnomalyLogDoc> findBySellerUuidOrderByDetectedAtDesc(String sellerUuid);

    Page<AnomalyLogDoc> findByAnomalyTypeAndSeverity(String anomalyType, String severity, Pageable pageable);
}
