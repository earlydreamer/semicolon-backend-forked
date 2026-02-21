package dukku.logconsumer.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * 이상거래 탐지 로그 MongoDB Document
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "anomaly_logs")
@CompoundIndex(def = "{'anomalyType': 1, 'severity': 1, 'detectedAt': -1}")
public class AnomalyLogDoc {

    @Id
    private String id;

    @Indexed
    private String settlementUuid;

    @Indexed
    private String sellerUuid;

    private String orderId;

    @Indexed
    private String anomalyType;

    @Indexed
    private String severity;

    private String description;

    private Long expectedValue;

    private Long actualValue;

    @Indexed(expireAfter = "90d")
    private Instant detectedAt;

    @Builder
    public AnomalyLogDoc(String settlementUuid, String sellerUuid, String orderId,
                          String anomalyType, String severity, String description,
                          Long expectedValue, Long actualValue, Instant detectedAt) {
        this.settlementUuid = settlementUuid;
        this.sellerUuid = sellerUuid;
        this.orderId = orderId;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.description = description;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.detectedAt = detectedAt;
    }
}
