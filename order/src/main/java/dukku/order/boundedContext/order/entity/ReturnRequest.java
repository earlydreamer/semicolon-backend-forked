package dukku.order.boundedContext.order.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.order.type.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnRequest extends BaseIdAndUUIDAndTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, comment = "구매자 UUID")
    private UUID userUuid;

    @Column(length = 50, comment = "반품 택배사 이름")
    private String carrierName;

    @Column(length = 20, comment = "반품 택배사 코드")
    private String carrierCode;

    @Column(length = 50, comment = "반품 운송장 번호")
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Column(length = 500, comment = "반품 사유")
    private String reason;

    @Column(comment = "판매자 수락 일시")
    private LocalDateTime approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> returnItems = new ArrayList<>();

    public void addReturnItem(ReturnItem item) {
        this.returnItems.add(item);
        item.setReturnRequest(this);
    }

    public static ReturnRequest create(Order order, UUID userUuid, String reason) {
        return ReturnRequest.builder()
                .order(order)
                .userUuid(userUuid)
                .reason(reason)
                .status(ReturnStatus.RETURN_REQUESTED)
                .build();
    }

    public void updateTrackingInfo(String carrierName, String carrierCode, String trackingNumber) {
        this.carrierName = carrierName;
        this.carrierCode = carrierCode;
        this.trackingNumber = trackingNumber;
        this.status = ReturnStatus.RETURN_SHIPPED;
    }

    public void approve() {
        this.status = ReturnStatus.RETURN_APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ReturnStatus.RETURN_COMPLETED;
    }

    public void reject() {
        this.status = ReturnStatus.RETURN_REJECTED;
    }

    public dukku.common.shared.order.dto.ReturnResponse toResponse() {
        return dukku.common.shared.order.dto.ReturnResponse.builder()
                .returnRequestUuid(this.getUuid())
                .orderUuid(this.order.getUuid())
                .status(this.status)
                .reason(this.reason)
                .carrierName(this.carrierName)
                .carrierCode(this.carrierCode)
                .trackingNumber(this.trackingNumber)
                .createdAt(this.getCreatedAt())
                .returnItems(this.returnItems.stream()
                        .map(item -> dukku.common.shared.order.dto.ReturnResponse.ReturnItemResponse.builder()
                                .returnItemUuid(item.getUuid())
                                .orderItemUuid(item.getOrderItem().getUuid())
                                .refundAmount(item.getRefundAmount())
                                .build())
                        .toList())
                .build();
    }
}
