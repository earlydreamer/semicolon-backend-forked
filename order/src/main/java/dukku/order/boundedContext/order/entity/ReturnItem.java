package dukku.order.boundedContext.order.entity;

import dukku.common.global.jpa.entity.BaseIdAndUUIDAndTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "return_items")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReturnItem extends BaseIdAndUUIDAndTime {

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false, comment = "환불될 금액")
    private int refundAmount;

    public static ReturnItem create(OrderItem orderItem, int refundAmount) {
        return ReturnItem.builder()
                .orderItem(orderItem)
                .refundAmount(refundAmount)
                .build();
    }
}
