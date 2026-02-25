package dukku.order.boundedContext.order.entity;

import dukku.common.shared.order.type.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderProductUuidMappingTest {

    @Test
    @DisplayName("getProductUuids 는 orderItemUuid 가 아니라 productUuid 를 반환한다")
    void getProductUuidsReturnsProductUuid() {
        UUID productUuid1 = UUID.randomUUID();
        UUID productUuid2 = UUID.randomUUID();

        Order order = Order.builder()
                .userUuid(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(10_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .build();

        OrderItem item1 = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(productUuid1)
                .sellerUuid(UUID.randomUUID())
                .productName("상품1")
                .productPrice(5_000)
                .build();

        OrderItem item2 = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(productUuid2)
                .sellerUuid(UUID.randomUUID())
                .productName("상품2")
                .productPrice(5_000)
                .build();

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        List<UUID> productUuids = order.getProductUuids();

        assertThat(productUuids).containsExactly(productUuid1, productUuid2);
    }
}