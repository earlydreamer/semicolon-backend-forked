package dukku.order.boundedContext.order.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnResponse.ReturnItemResponse;
import dukku.common.shared.order.type.ReturnStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 반품 요청 엔티티
 */
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

    @Column(length = 500, comment = "반품 요청 사유")
    private String reason;

    @Column(length = 500, comment = "반품 거절 사유")
    private String rejectionReason;

    @Column(comment = "최종 반품 승인 일시")
    private LocalDateTime approvedAt;

    @Builder.Default
    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnItem> returnItems = new ArrayList<>();

    /**
     * 반품 아이템 연관관계 설정
     */
    public void addReturnItem(ReturnItem item) {
        this.returnItems.add(item);
        item.setReturnRequest(this);
    }

    /**
     * 반품 요청 엔티티 생성
     */
    public static ReturnRequest create(Order order, UUID userUuid, String reason) {
        return ReturnRequest.builder()
                .order(order)
                .userUuid(userUuid)
                .reason(reason)
                .status(ReturnStatus.RETURN_REQUESTED)
                .build();
    }

    /**
     * 판매자 1차 승인 상태 전이
     */
    public void approveBySeller() {
        this.status = ReturnStatus.RETURN_SELLER_APPROVED;
    }

    /**
     * 반품 운송장 정보 등록 및 발송 상태 전이
     */
    public void updateTrackingInfo(String carrierName, String carrierCode, String trackingNumber) {
        this.carrierName = carrierName;
        this.carrierCode = carrierCode;
        this.trackingNumber = trackingNumber;
        this.status = ReturnStatus.RETURN_SHIPPED;
    }

    /**
     * 판매자 최종 승인 상태 전이
     */
    public void approveFinal() {
        this.status = ReturnStatus.RETURN_APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 반품 완료 상태 전이
     */
    public void complete() {
        this.status = ReturnStatus.RETURN_COMPLETED;
    }

    /**
     * 반품 발송 전 거절 상태 전이
     */
    public void rejectBeforeShipment(String rejectionReason) {
        this.status = ReturnStatus.RETURN_REJECTED_BEFORE_SHIPMENT;
        this.rejectionReason = rejectionReason;
    }

    /**
     * 반품 발송 후 거절 상태 전이
     */
    public void rejectAfterShipment(String rejectionReason) {
        this.status = ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT;
        this.rejectionReason = rejectionReason;
    }

    /**
     * 반품 응답 DTO 변환
     */
    public ReturnResponse toResponse() {
        return ReturnResponse.builder()
                .returnRequestUuid(this.getUuid())
                .orderUuid(this.order.getUuid())
                .status(this.status)
                .reason(this.reason)
                .rejectionReason(this.rejectionReason)
                .carrierName(this.carrierName)
                .carrierCode(this.carrierCode)
                .trackingNumber(this.trackingNumber)
                .createdAt(this.getCreatedAt())
                .returnItems(this.returnItems.stream()
                        .map(item -> ReturnItemResponse.builder()
                                .returnItemUuid(item.getUuid())
                                .orderItemUuid(item.getOrderItem().getUuid())
                                .refundAmount(item.getRefundAmount())
                                .build())
                        .toList())
                .build();
    }
}
