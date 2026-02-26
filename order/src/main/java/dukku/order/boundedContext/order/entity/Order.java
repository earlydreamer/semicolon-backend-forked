package dukku.order.boundedContext.order.entity;

import dukku.common.global.exception.ConflictException;
import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import dukku.common.shared.order.dto.OrderCreateRequest;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.dto.OrderResponse;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseIdAndUUIDAndTime {
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, comment = "구매자 UUID")
    private UUID userUuid;

    @Column(nullable = false, comment = "수령 총 금액")
    private int totalAmount;

    @Column(nullable = false, comment = "수령 주소")
    private String address;

    @Column(nullable = false, length = 50, comment = "수령인")
    private String recipient;

    @Column(nullable = false, length = 50, comment = "수령인 연락처")
    private String contactNumber;

    private int refundedAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // 주문 항목 추가와 주문-상품 연관관계 동기화
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    // 주문 생성 요청 기준 총 주문 금액 계산, 환불 누적 0 초기화
    public static Order createOrder(OrderCreateRequest request, UUID userUuid) {
        int totalAmount = request.getItems().stream()
                .mapToInt(OrderCreateRequest.OrderItemCreateRequest::getProductPrice)
                .sum();

        return Order.builder()
                .userUuid(userUuid)
                .totalAmount(totalAmount)
                .address(request.getAddress())
                .recipient(request.getRecipient())
                .contactNumber(request.getContactNumber())
                .refundedAmount(0)
                .status(OrderStatus.PENDING)
                .build();
    }

    // 주문 상태 전환
    public void updateOrderStatus(OrderStatus status) {
        this.status = status;
    }

    // 배송지 정보 변경 가능 조건 검사 후 사용자 배송 정보 반영
    public void updateOrderForUser(String address, String recipient, String contactNumber) {
        if (!isShipped()) {
            throw new ConflictException("이미 배송 준비 중이거나 완료된 상품이 있어 배송지를 변경할 수 없습니다.");
        }

        this.address = address;
        this.recipient = recipient;
        this.contactNumber = contactNumber;
    }

    // 환불 누적치 갱신(음수/0 무시, 총액 초과분은 총액 상한)
    public void updateRefundedAmount(int refundedAmount) {
        if (refundedAmount <= 0) {
            return;
        }
        long accumulated = (long) this.refundedAmount + refundedAmount;
        this.refundedAmount = (int) Math.min(accumulated, this.totalAmount);
    }

    // 주문 상세 응답 DTO 변환
    public static OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderUuid(order.getUuid())
                .userUuid(order.getUserUuid())
                .totalAmount(order.getTotalAmount())
                .refundedAmount(order.getRefundedAmount())
                .orderStatus(order.getStatus())
                .orderedAt(order.getCreatedAt())
                .recipient(order.getRecipient())
                .contactNumber(order.getContactNumber())
                .address(order.getAddress())
                .items(order.getOrderItems().stream()
                        .map(Order::fromOrderItemResponse)
                        .toList())
                .build();
    }

    // 주문 항목 상태가 모두 변경 가능이면 true
    public boolean isShipped() {
        return orderItems.stream()
                .map(OrderItem::getStatus)
                .allMatch(OrderItemStatus::canChangeShippingInfo);
    }

    // 주문 상품 UUID 목록 추출
    public List<UUID> getProductUuids() {
        return orderItems.stream()
                .map(OrderItem::getProductUuid)
                .toList();
    }

    // 주문 목록 응답 DTO 변환
    public static OrderListResponse fromOrderListResponse(Order order) {
        return OrderListResponse.builder()
                .orderUuid(order.getUuid())
                .orderDate(order.getCreatedAt())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getOrderItems().stream()
                        .map(Order::fromSimpleOrderItemResponse)
                        .toList())
                .build();
    }

    // 주문 상세 항목 응답 DTO 변환
    public static OrderResponse.OrderItemResponse fromOrderItemResponse(OrderItem item) {
        return OrderResponse.OrderItemResponse.builder()
                .orderItemUuid(item.getUuid())
                .productUuid(item.getProductUuid())
                .sellerUuid(item.getSellerUuid())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice())
                .imageUrl(item.getImageUrl())
                .itemStatus(item.getStatus())
                .carrierName(item.getCarrierName())
                .trackingNumber(item.getTrackingNumber())
                .build();
    }

    // 주문 목록용 항목 응답 DTO 변환
    public static OrderListResponse.SimpleOrderItemResponse fromSimpleOrderItemResponse(OrderItem item) {
        return OrderListResponse.SimpleOrderItemResponse.builder()
                .orderItemUuid(item.getUuid())
                .productUuid(item.getProductUuid())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice())
                .imageUrl(item.getImageUrl())
                .itemStatus(item.getStatus())
                .carrierName(item.getCarrierName())
                .trackingNumber(item.getTrackingNumber())
                .build();
    }
}
