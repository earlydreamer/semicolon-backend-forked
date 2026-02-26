package dukku.order.boundedContext.order.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:return_process_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
class ReturnProcessIntegrationTest {

    @Autowired
    private RequestReturnUseCase requestReturnUseCase;

    @Autowired
    private SellerApproveReturnUseCase sellerApproveReturnUseCase;

    @Autowired
    private SellerRejectReturnUseCase sellerRejectReturnUseCase;

    @Autowired
    private RegisterReturnTrackingUseCase registerReturnTrackingUseCase;

    @Autowired
    private ApproveReturnUseCase approveReturnUseCase;

    @Autowired
    private FinalRejectReturnUseCase finalRejectReturnUseCase;

    @Autowired
    private SellerReceiveReturnUseCase sellerReceiveReturnUseCase;

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
        reset(eventPublisher);
    }

    @Test
    @DisplayName("반품 신청부터 최종 승인까지 전체 플로우가 순서대로 진행된다")
    void fullFlowToFinalApprove() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("상품 하자")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());
        assertThat(requested.getStatus()).isEqualTo(ReturnStatus.RETURN_REQUESTED);

        ReturnResponse sellerApproved = sellerApproveReturnUseCase.execute(fixture.firstSellerUuid(), requested.getReturnRequestUuid());
        assertThat(sellerApproved.getStatus()).isEqualTo(ReturnStatus.RETURN_SELLER_APPROVED);

        ReturnResponse shipped = registerReturnTrackingUseCase.execute(
                fixture.buyerUuid(),
                requested.getReturnRequestUuid(),
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ대한통운")
                        .carrierCode("04")
                        .trackingNumber("1234567890")
                        .build());
        assertThat(shipped.getStatus()).isEqualTo(ReturnStatus.RETURN_SHIPPED);
        assertThat(shipped.getTrackingNumber()).isEqualTo("1234567890");

        ReturnResponse received = sellerReceiveReturnUseCase.execute(
                fixture.firstSellerUuid(),
                requested.getReturnRequestUuid());
        assertThat(received.getStatus()).isEqualTo(ReturnStatus.RETURN_RECEIVED);

        ReturnResponse finalApproved = approveReturnUseCase.execute(fixture.firstSellerUuid(), requested.getReturnRequestUuid());
        assertThat(finalApproved.getStatus()).isEqualTo(ReturnStatus.RETURN_APPROVED);

        Order order = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(order.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.REFUND_IN_PROGRESS);
    }

    @Test
    @DisplayName("판매자가 반품 발송 전에 거절하면 반품 상태와 아이템 상태가 복구된다")
    void sellerRejectBeforeShipment() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("단순 변심")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());

        ReturnResponse rejected = sellerRejectReturnUseCase.execute(
                fixture.firstSellerUuid(),
                requested.getReturnRequestUuid(),
                "반품 사유 불충분");

        assertThat(rejected.getStatus()).isEqualTo(ReturnStatus.RETURN_REJECTED_BEFORE_SHIPMENT);
        assertThat(rejected.getRejectionReason()).isEqualTo("반품 사유 불충분");

        Order order = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(order.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
    }

    @Test
    @DisplayName("판매자 1차 승인 이후에도 발송 전이면 판매자는 1차 거절할 수 있다")
    void sellerRejectAfterSellerApproveBeforeShipment() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("사이즈 불만")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());

        sellerApproveReturnUseCase.execute(fixture.firstSellerUuid(), requested.getReturnRequestUuid());
        ReturnResponse rejected = sellerRejectReturnUseCase.execute(
                fixture.firstSellerUuid(),
                requested.getReturnRequestUuid(),
                "검수 결과 반품 불가");

        assertThat(rejected.getStatus()).isEqualTo(ReturnStatus.RETURN_REJECTED_BEFORE_SHIPMENT);
        assertThat(rejected.getRejectionReason()).isEqualTo("검수 결과 반품 불가");
    }

    @Test
    @DisplayName("판매자가 최종 거절하면 발송 후 거절 상태로 전환되고 아이템은 배송완료로 복구된다")
    void sellerFinalRejectAfterShipment() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("상품 하자")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());

        sellerApproveReturnUseCase.execute(fixture.firstSellerUuid(), requested.getReturnRequestUuid());
        registerReturnTrackingUseCase.execute(
                fixture.buyerUuid(),
                requested.getReturnRequestUuid(),
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ대한통운")
                .carrierCode("04")
                .trackingNumber("9999999999")
                .build());

        ReturnResponse received = sellerReceiveReturnUseCase.execute(
                fixture.firstSellerUuid(),
                requested.getReturnRequestUuid());
        assertThat(received.getStatus()).isEqualTo(ReturnStatus.RETURN_RECEIVED);

        ReturnResponse rejected = finalRejectReturnUseCase.execute(
                fixture.firstSellerUuid(),
                requested.getReturnRequestUuid(),
                "회수 상품 상태 불량");

        assertThat(rejected.getStatus()).isEqualTo(ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT);
        assertThat(rejected.getRejectionReason()).isEqualTo("회수 상품 상태 불량");

        Order order = orderRepository.findByUuidWithItems(fixture.orderUuid()).orElseThrow();
        assertThat(order.getOrderItems().get(0).getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
    }

    @Test
    @DisplayName("판매자 1차 승인 전에는 구매자가 운송장을 등록할 수 없다")
    void denyTrackingRegistrationBeforeSellerApprove() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("상품 하자")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());

        assertThatThrownBy(() -> registerReturnTrackingUseCase.execute(
                fixture.buyerUuid(),
                requested.getReturnRequestUuid(),
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ대한통운")
                        .carrierCode("04")
                        .trackingNumber("1111111111")
                        .build()))
                .isInstanceOf(ReturnRequestStatusInvalidException.class);
    }

    @Test
    @DisplayName("타 판매자는 반품 승인이나 거절을 처리할 수 없다")
    void denyOtherSellerApprovalOrRejection() {
        ReturnFixture fixture = createSingleSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("상품 하자")
                        .orderItemUuids(List.of(fixture.firstItemUuid()))
                        .build());

        UUID otherSellerUuid = UUID.randomUUID();
        assertThatThrownBy(() -> sellerApproveReturnUseCase.execute(otherSellerUuid, requested.getReturnRequestUuid()))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);

        assertThatThrownBy(() -> sellerRejectReturnUseCase.execute(otherSellerUuid, requested.getReturnRequestUuid(), "권한 없음"))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);
    }

    @Test
    @DisplayName("서로 다른 판매자 아이템을 한 번에 반품 신청하면 단일 판매자는 승인할 수 없다")
    void denySellerApproveWhenRequestContainsOtherSellerItems() {
        ReturnFixture fixture = createMultiSellerDeliveredOrderFixture();

        ReturnResponse requested = requestReturnUseCase.execute(
                fixture.buyerUuid(),
                fixture.orderUuid(),
                ReturnRequestCreateDto.builder()
                        .reason("복수 상품 반품")
                        .orderItemUuids(List.of(fixture.firstItemUuid(), fixture.secondItemUuid()))
                        .build());

        assertThatThrownBy(() -> sellerApproveReturnUseCase.execute(fixture.firstSellerUuid(), requested.getReturnRequestUuid()))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);

        ReturnRequest found = returnRequestRepository.findByUuid(requested.getReturnRequestUuid()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ReturnStatus.RETURN_REQUESTED);
    }

    private ReturnFixture createSingleSellerDeliveredOrderFixture() {
        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();

        Order order = Order.builder()
                .userUuid(buyerUuid)
                .totalAmount(15_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-1111-2222")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem item = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(sellerUuid)
                .productName("테스트 상품")
                .productPrice(15_000)
                .imageUrl("https://example.com/item.png")
                .status(OrderItemStatus.DELIVERED)
                .build();

        order.addOrderItem(item);
        Order savedOrder = orderRepository.save(order);

        return new ReturnFixture(
                buyerUuid,
                sellerUuid,
                null,
                savedOrder.getUuid(),
                savedOrder.getOrderItems().get(0).getUuid(),
                null
        );
    }

    private ReturnFixture createMultiSellerDeliveredOrderFixture() {
        UUID buyerUuid = UUID.randomUUID();
        UUID firstSellerUuid = UUID.randomUUID();
        UUID secondSellerUuid = UUID.randomUUID();

        Order order = Order.builder()
                .userUuid(buyerUuid)
                .totalAmount(30_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-3333-4444")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem firstItem = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(firstSellerUuid)
                .productName("첫 번째 상품")
                .productPrice(10_000)
                .status(OrderItemStatus.DELIVERED)
                .build();

        OrderItem secondItem = OrderItem.builder()
                .productUuid(UUID.randomUUID())
                .sellerUuid(secondSellerUuid)
                .productName("두 번째 상품")
                .productPrice(20_000)
                .status(OrderItemStatus.DELIVERED)
                .build();

        order.addOrderItem(firstItem);
        order.addOrderItem(secondItem);
        Order savedOrder = orderRepository.save(order);

        return new ReturnFixture(
                buyerUuid,
                firstSellerUuid,
                secondSellerUuid,
                savedOrder.getUuid(),
                savedOrder.getOrderItems().get(0).getUuid(),
                savedOrder.getOrderItems().get(1).getUuid()
        );
    }

    private record ReturnFixture(
            UUID buyerUuid,
            UUID firstSellerUuid,
            UUID secondSellerUuid,
            UUID orderUuid,
            UUID firstItemUuid,
            UUID secondItemUuid
    ) {
    }
}
