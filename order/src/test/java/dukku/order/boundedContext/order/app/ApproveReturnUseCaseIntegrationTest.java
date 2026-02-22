package dukku.order.boundedContext.order.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnItem;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:approve_return_use_case_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.elasticsearch.uris=http://localhost:9200",
        "jwt.access.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktYWNjZXNzLTAxMjM=",
        "jwt.refresh.secret.key=dGhpcy1rZXktaXMtdGVzdC1rZXktcmVmcmVzaC0wMTI=",
        "crypto.key=dGhpcy1rZXktaXMtdGVzdC1rZXktY3J5cHRvLTAxMjM="
})
@Transactional
class ApproveReturnUseCaseIntegrationTest {

    @Autowired
    private ApproveReturnUseCase useCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        returnRequestRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("최종 승인 시 모든 반품 아이템의 판매자가 본인이면 승인되고 이벤트가 발행된다")
    void approvesWhenSellerOwnsAllItems() {
        UUID sellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(sellerUuid, sellerUuid);

        useCase.execute(sellerUuid, returnRequest.getUuid());

        ReturnRequest approved = returnRequestRepository.findByUuid(returnRequest.getUuid()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(ReturnStatus.RETURN_APPROVED);

        ArgumentCaptor<PartialRefundRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PartialRefundRequestedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().returnRequestUuid()).isEqualTo(returnRequest.getUuid());

        assertThat(approved.getReturnItems())
                .extracting(item -> item.getOrderItem().getStatus())
                .containsOnly(OrderItemStatus.REFUND_IN_PROGRESS);
    }

    @Test
    @DisplayName("최종 승인 시 타 판매자 아이템이 포함되어 있으면 승인할 수 없다")
    void deniesWhenOtherSellerItemExists() {
        UUID sellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(sellerUuid, UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(sellerUuid, returnRequest.getUuid()))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);

        ReturnRequest denied = returnRequestRepository.findByUuid(returnRequest.getUuid()).orElseThrow();
        assertThat(denied.getStatus()).isEqualTo(ReturnStatus.RETURN_SHIPPED);
        verify(eventPublisher, never()).publish(any());
    }

    private ReturnRequest createReturnRequest(UUID firstSellerUuid, UUID secondSellerUuid) {
        UUID buyerUuid = UUID.randomUUID();

        Order order = Order.builder()
                .userUuid(buyerUuid)
                .totalAmount(30_000)
                .address("seoul")
                .recipient("buyer")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem firstItem = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(firstSellerUuid)
                .productName("item-1")
                .productPrice(10_000)
                .status(OrderItemStatus.REFUND_REQUESTED)
                .build();
        OrderItem secondItem = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(secondSellerUuid)
                .productName("item-2")
                .productPrice(20_000)
                .status(OrderItemStatus.REFUND_REQUESTED)
                .build();

        order.addOrderItem(firstItem);
        order.addOrderItem(secondItem);
        Order savedOrder = orderRepository.save(order);

        ReturnRequest returnRequest = ReturnRequest.create(savedOrder, buyerUuid, "단순 변심");
        returnRequest.approveBySeller();
        returnRequest.updateTrackingInfo("cj", "04", "1234567890");

        List<OrderItem> savedItems = savedOrder.getOrderItems();
        returnRequest.addReturnItem(ReturnItem.create(savedItems.get(0), 10_000));
        returnRequest.addReturnItem(ReturnItem.create(savedItems.get(1), 20_000));

        return returnRequestRepository.save(returnRequest);
    }
}
