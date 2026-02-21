package dukku.logconsumer.controller;

import dukku.logconsumer.document.AnomalyLogDoc;
import dukku.logconsumer.repository.AnomalyLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 이상거래 탐지 이력 조회 API
 */
@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyQueryController {

    private final AnomalyLogRepository anomalyLogRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 특정 정산의 이상거래 이력 조회
     */
    @GetMapping("/settlement/{settlementUuid}")
    public List<AnomalyLogDoc> getBySettlementUuid(@PathVariable String settlementUuid) {
        return anomalyLogRepository.findBySettlementUuidOrderByDetectedAtDesc(settlementUuid);
    }

    /**
     * 특정 셀러의 이상거래 이력 조회
     */
    @GetMapping("/seller/{sellerUuid}")
    public List<AnomalyLogDoc> getBySellerUuid(@PathVariable String sellerUuid) {
        return anomalyLogRepository.findBySellerUuidOrderByDetectedAtDesc(sellerUuid);
    }

    /**
     * 타입/심각도/기간 필터 조회 (페이징)
     */
    @GetMapping
    public Page<AnomalyLogDoc> getAnomalies(
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "detectedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Query query = new Query().with(pageable);

        if (anomalyType != null) {
            query.addCriteria(Criteria.where("anomalyType").is(anomalyType));
        }
        if (severity != null) {
            query.addCriteria(Criteria.where("severity").is(severity));
        }
        if (from != null && to != null) {
            query.addCriteria(Criteria.where("detectedAt").gte(from).lte(to));
        } else if (from != null) {
            query.addCriteria(Criteria.where("detectedAt").gte(from));
        } else if (to != null) {
            query.addCriteria(Criteria.where("detectedAt").lte(to));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AnomalyLogDoc.class);
        List<AnomalyLogDoc> content = mongoTemplate.find(query, AnomalyLogDoc.class);

        return new PageImpl<>(content, pageable, total);
    }
}
