package dukku.order.boundedContext.order.entity;

import dukku.common.global.exception.ConflictException;
import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.order.dto.ConfirmedOrderItemResponse;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.dto.DeliveryInfoRequest;
import dukku.common.shared.order.dto.OrderCreateRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseIdAndUUIDAndTime {
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID productUuid;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID sellerUuid;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private int productPrice;

    private String imageUrl;

    @Column(length = 50, comment = "택배사 이름")
    private String carrierName;

    @Column(length = 20, comment = "택배사 코드")
    private String carrierCode;

    @Column(length = 50, comment = "운송장 번호")
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;

    private LocalDateTime deliveryDate;

    @Column(comment = "구매 확정 일시")
    private LocalDateTime confirmedAt;

    public static OrderItem createOrderItem(OrderCreateRequest.OrderItemCreateRequest request) {
        return OrderItem.builder()
                .productUuid(request.getProductUuid())
                .sellerUuid(request.getSellerUuid())
                .productName(request.getProductName())
                .productPrice(request.getProductPrice())
                .imageUrl(request.getImageUrl())
                .build();
    }

    public void updateDeliveryInfo(DeliveryInfoRequest request) {
        this.carrierName = request.getCarrierName();
        this.carrierCode = request.getCarrierCode();
        this.trackingNumber = request.getTrackingNumber();
        if (this.status == null || this.status == OrderItemStatus.PAYMENT_COMPLETED) {
            this.status = OrderItemStatus.PREPARING_SHIPMENT;
        }
    }

    public void updateOrderStatus(OrderItemStatus newStatus) {
        if (this.status == newStatus)
            return;

        validateStateTransition(newStatus);
        this.status = newStatus;
        if (newStatus == OrderItemStatus.SHIPPED) {
            this.deliveryDate = LocalDateTime.now();
        }
    }

    public void forceUpdateOrderStatusForAdmin(OrderItemStatus newStatus) {
        if (newStatus == null || this.status == newStatus) {
            return;
        }
        this.status = newStatus;
        if (newStatus == OrderItemStatus.SHIPPED) {
            this.deliveryDate = LocalDateTime.now();
        }
        if (newStatus == OrderItemStatus.CONFIRMED && this.confirmedAt == null) {
            this.confirmedAt = LocalDateTime.now();
        }
    }

    private void validateStateTransition(OrderItemStatus newStatus) {
        switch (newStatus) {
            case CANCELED -> {
                // 이미 배송 중이거나 배송 완료된 상품은 취소 불가 (반품 절차 밟아야 함)
                if (isShippingOrCompleted()) {
                    throw new ConflictException("이미 배송이 시작되었거나 완료된 상품은 취소할 수 없습니다. 반품을 이용해주세요.");
                }
                // 구매 확정된 상품 취소 불가
                if (this.status == OrderItemStatus.CONFIRMED) {
                    throw new ConflictException("이미 구매 확정된 상품은 취소할 수 없습니다.");
                }
            }
            case CONFIRMED -> {
                // 배송 완료 상태가 아니면 구매 확정 불가
                if (this.status != OrderItemStatus.DELIVERED) {
                    throw new ConflictException("배송이 완료된 상품만 구매 확정할 수 있습니다.");
                }

                this.confirmedAt = LocalDateTime.now();
            }
            case REFUND_REQUESTED -> {
                // 배송 전 상품은 취소(CANCEL_REQUESTED) 대상이며, 배송이 시작된 이후에만 반품 가능
                if (!isShippingOrCompleted()) {
                    throw new ConflictException("아직 배송되지 않은 상품입니다. 주문 취소를 이용해주세요.");
                }

                // 이미 구매 확정이 이뤄진 경우 반품/환불 불가
                if (this.status == OrderItemStatus.CONFIRMED) {
                    throw new ConflictException("구매 확정 후에는 반품/환불 신청이 불가능합니다.");
                }
            }
            // 그 외 관리자용 상태 변경(배송중 등)은 허용하거나 별도 로직 추가
        }
    }

    private boolean isShippingOrCompleted() {
        return this.status == OrderItemStatus.SHIPPED ||
                this.status == OrderItemStatus.DELIVERED;
    }

    // 가상 배송 스케줄러용 상태 변경 메서드
    public void updateMockDeliveryStatus(OrderItemStatus nextStatus) {
        // 최종 상태 도달 시 변경 불가
        if (this.status == OrderItemStatus.CONFIRMED ||
                this.status == OrderItemStatus.DELIVERED ||
                this.status == OrderItemStatus.CANCELED) {
            return;
        }

        this.status = nextStatus;
        this.deliveryDate = LocalDateTime.now(); // 상태 변경 시점 갱신 (다음 단계 카운트다운 시작)
    }

    public static ConfirmedOrderItemResponse toConfirmedOrderItemResponse(OrderItem orderItem) {
        return new ConfirmedOrderItemResponse(
                orderItem.getUuid(),
                orderItem.getOrder().getUuid(),
                orderItem.getOrder().getUserUuid(),
                orderItem.getSellerUuid(),
                orderItem.getProductUuid(),
                orderItem.getProductName(),
                orderItem.getProductPrice(),
                orderItem.getConfirmedAt());
    }
}
