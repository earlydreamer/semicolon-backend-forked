package dukku.order.boundedContext.order.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.common.shared.payment.event.RefundCompletedEvent;
import dukku.common.shared.payment.event.RefundFailedEvent;
import dukku.common.shared.payment.type.PaymentFailureCode;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.in.OrderEventListener;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ProcessedRefundEventRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:return_refund_flow_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class ReturnRefundFlowIntegrationTest {

    @Autowired
    private RequestReturnUseCase requestReturnUseCase;

    @Autowired
    private SellerApproveReturnUseCase sellerApproveReturnUseCase;

    @Autowired
    private RegisterReturnTrackingUseCase registerReturnTrackingUseCase;

    @Autowired
    private ApproveReturnUseCase approveReturnUseCase;

    @Autowired
    private OrderEventListener orderEventListener;

    @Autowired
    private SellerReceiveReturnUseCase sellerReceiveReturnUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ProcessedRefundEventRepository processedRefundEventRepository;

    @MockitoBean
    private EventPublisher eventPublisher;

    @BeforeEach
    void cleanUp() {
        processedRefundEventRepository.deleteAll();
        returnRequestRepository.deleteAll();
        orderRepository.deleteAll();
        reset(eventPublisher);
    }

    @Test
    @DisplayName("반품 플로우 완료 후 중복 환불 완료 이벤트는 무시된다")
    void completesReturnFlowAndIgnoresDuplicateRefundEvent() {
        ReturnFlowFixture fixture = createDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("단순 변심")
                        .orderItemUuids(List.of(fixture.orderItemUuid()))
                        .build());
        assertThat(requested.getStatus()).isEqualTo(ReturnStatus.RETURN_REQUESTED);

        ReturnResponse sellerApproved = sellerApproveReturnUseCase.execute(fixture.sellerUuid(), requested.getReturnRequestUuid());
        assertThat(sellerApproved.getStatus()).isEqualTo(ReturnStatus.RETURN_SELLER_APPROVED);

        ReturnResponse shipped = registerReturnTrackingUseCase.execute(
                fixture.buyerUuid(),
                requested.getReturnRequestUuid(),
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ")
                        .carrierCode("04")
                        .trackingNumber("1234567890")
                        .build());
        assertThat(shipped.getStatus()).isEqualTo(ReturnStatus.RETURN_SHIPPED);
        ReturnResponse received = sellerReceiveReturnUseCase.execute(
                fixture.sellerUuid(),
                requested.getReturnRequestUuid());
        assertThat(received.getStatus()).isEqualTo(ReturnStatus.RETURN_RECEIVED);

        Order requestedOrder = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(requestedOrder.getOrderItems()).hasSize(1);
        assertThat(requestedOrder.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.REFUND_IN_PROGRESS);

        ReturnResponse approved = approveReturnUseCase.execute(fixture.sellerUuid(), requested.getReturnRequestUuid());
        assertThat(approved.getStatus()).isEqualTo(ReturnStatus.RETURN_APPROVED);

        Order inProgressOrder = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(inProgressOrder.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.REFUND_IN_PROGRESS);

        ArgumentCaptor<PartialRefundRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PartialRefundRequestedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        PartialRefundRequestedEvent partialRefundRequestedEvent = eventCaptor.getValue();
        long refundAmount = partialRefundRequestedEvent.refundItems().stream()
                .mapToLong(PartialRefundRequestedEvent.RefundItemInfo::refundAmount)
                .sum();

        UUID refundUuid = UUID.randomUUID();
        RefundCompletedEvent completedEvent = new RefundCompletedEvent(
                refundUuid,
                UUID.randomUUID(),
                fixture.orderUuid(),
                refundAmount,
                0L,
                fixture.buyerUuid(),
                LocalDateTime.now(),
                List.of(fixture.orderItemUuid()));

        orderEventListener.handle(completedEvent);

        Order refundedOrder = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(refundedOrder.getRefundedAmount()).isEqualTo((int) refundAmount);
        assertThat(refundedOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(refundedOrder.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.REFUND_COMPLETED);

        ReturnRequest completedRequest = returnRequestRepository.findByUuid(approved.getReturnRequestUuid()).orElseThrow();
        assertThat(completedRequest.getStatus()).isEqualTo(ReturnStatus.RETURN_COMPLETED);

        orderEventListener.handle(completedEvent);

        Order retriedOrder = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(retriedOrder.getRefundedAmount()).isEqualTo((int) refundAmount);
        assertThat(retriedOrder.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.REFUND_COMPLETED);
        assertThat(processedRefundEventRepository.existsByRefundUuid(refundUuid)).isTrue();
    }

    @Test
    @DisplayName("환불 실패 이벤트 수신 시 최종 승인된 반품은 발송 후 거절 상태로 전환된다")
    void revertsApprovedReturnWhenRefundFails() {
        ReturnFlowFixture fixture = createDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("defect")
                        .orderItemUuids(List.of(fixture.orderItemUuid()))
                        .build());

        sellerApproveReturnUseCase.execute(fixture.sellerUuid(), requested.getReturnRequestUuid());
        registerReturnTrackingUseCase.execute(
                fixture.buyerUuid(),
                requested.getReturnRequestUuid(),
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ")
                .carrierCode("04")
                .trackingNumber("1234567890")
                .build());
        ReturnResponse received2 = sellerReceiveReturnUseCase.execute(
                fixture.sellerUuid(),
                requested.getReturnRequestUuid());
        assertThat(received2.getStatus()).isEqualTo(ReturnStatus.RETURN_RECEIVED);

        ReturnResponse approved = approveReturnUseCase.execute(fixture.sellerUuid(), requested.getReturnRequestUuid());
        assertThat(approved.getStatus()).isEqualTo(ReturnStatus.RETURN_APPROVED);

        RefundFailedEvent failedEvent = new RefundFailedEvent(
                fixture.orderUuid(),
                UUID.randomUUID(),
                fixture.buyerUuid(),
                fixture.itemPrice().longValue(),
                fixture.itemPrice().longValue(),
                0L,
                PaymentFailureCode.REFUND_PG_CANCEL_FAILED,
                true,
                "simulated pg failure",
                LocalDateTime.now());

        orderEventListener.handle(failedEvent);

        Order restoredOrder = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(restoredOrder.getRefundedAmount()).isZero();
        assertThat(restoredOrder.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.DELIVERED);

        ReturnRequest rejectedRequest = returnRequestRepository.findByUuid(approved.getReturnRequestUuid()).orElseThrow();
        assertThat(rejectedRequest.getStatus()).isEqualTo(ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT);
    }

    private ReturnFlowFixture createDeliveredOrderFixture() {
        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();

        Order order = Order.builder()
                .userUuid(buyerUuid)
                .totalAmount(15_000)
                .address("seoul")
                .recipient("buyer")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(sellerUuid)
                .productName("test-item")
                .productPrice(15_000)
                .imageUrl("https://example.com/item.png")
                .status(OrderItemStatus.DELIVERED)
                .build();

        order.addOrderItem(orderItem);
        Order savedOrder = orderRepository.save(order);

        UUID orderUuid = savedOrder.getUuid();
        UUID orderItemUuid = savedOrder.getOrderItems().get(0).getUuid();

        return new ReturnFlowFixture(buyerUuid, sellerUuid, orderUuid, orderItemUuid, 15_000);
    }

    private record ReturnFlowFixture(
            UUID buyerUuid,
            UUID sellerUuid,
            UUID orderUuid,
            UUID orderItemUuid,
            Integer itemPrice
    ) {
    }
}
