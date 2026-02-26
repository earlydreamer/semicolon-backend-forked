package dukku.order.boundedContext.order.entity;

import dukku.common.shared.order.dto.DeliveryInfoRequest;
import dukku.common.shared.order.type.OrderItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemDeliveryInfoTest {

    @Test
    @DisplayName("status가 null이어도 운송장 입력 시 PREPARING_SHIPMENT로 전이된다")
    void updateDeliveryInfoNullStatusToPreparing() {
        OrderItem item = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("test-product")
                .productPrice(1000)
                .status(null)
                .build();

        DeliveryInfoRequest request = new DeliveryInfoRequest();
        ReflectionTestUtils.setField(request, "carrierName", "CJ대한통운");
        ReflectionTestUtils.setField(request, "carrierCode", "cj");
        ReflectionTestUtils.setField(request, "trackingNumber", "111122223333");
        item.updateDeliveryInfo(request);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PREPARING_SHIPMENT);
        assertThat(item.getTrackingNumber()).isEqualTo("111122223333");
    }

    @Test
    @DisplayName("status가 PAYMENT_COMPLETED이면 운송장 입력 시 PREPARING_SHIPMENT로 전이된다")
    void updateDeliveryInfoPaymentCompletedToPreparing() {
        OrderItem item = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("test-product")
                .productPrice(1000)
                .status(OrderItemStatus.PAYMENT_COMPLETED)
                .build();

        DeliveryInfoRequest request = new DeliveryInfoRequest();
        ReflectionTestUtils.setField(request, "carrierName", "CJ대한통운");
        ReflectionTestUtils.setField(request, "carrierCode", "cj");
        ReflectionTestUtils.setField(request, "trackingNumber", "444455556666");
        item.updateDeliveryInfo(request);

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PREPARING_SHIPMENT);
    }
}
