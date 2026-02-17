package dukku.deposit.boundedContext.deposit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "processed_refund_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_refund_events_refund_uuid", columnNames = "refund_uuid")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
/**
 * 이미 처리된 환불 이벤트를 추적해 중복 반영을 방지하기 위한 엔티티
 */
public class ProcessedRefundEvent {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(nullable = false, updatable = false)
    private Integer id;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "refund_uuid", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID refundUuid;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "order_uuid", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID orderUuid;

    @Column(name = "refund_amount", nullable = false, updatable = false)
    private Long refundAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ProcessedRefundEvent create(UUID refundUuid, UUID orderUuid, Long refundAmount) {
        return ProcessedRefundEvent.builder()
                .refundUuid(refundUuid)
                .orderUuid(orderUuid)
                .refundAmount(refundAmount)
                .build();
    }
}
