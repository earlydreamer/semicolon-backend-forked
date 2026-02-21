package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.common.shared.order.exception.OrderItemNotFoundException;
import dukku.common.shared.order.exception.OrderNotFoundException;
import dukku.common.shared.order.exception.ReturnItemSelectionRequiredException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.OrderRepository;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestReturnUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private RequestReturnUseCase useCase;

    @Test
    @DisplayName("반품 신청이 정상 접수되면 요청 상태가 생성되고 아이템은 환불요청 상태가 된다")
    void requestReturnSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = createOrder(orderUuid, userUuid);
        UUID firstItemUuid = order.getOrderItems().get(0).getUuid();
        UUID secondItemUuid = order.getOrderItems().get(1).getUuid();

        ReturnRequestCreateDto dto = ReturnRequestCreateDto.builder()
                .reason("상품 하자")
                .orderItemUuids(List.of(firstItemUuid, secondItemUuid))
                .build();

        when(orderRepository.findByUuidWithItems(orderUuid)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResponse response = useCase.execute(userUuid, orderUuid, dto);

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.RETURN_REQUESTED);
        assertThat(response.getReturnItems()).hasSize(2);
        assertThat(order.getOrderItems())
                .extracting(OrderItem::getStatus)
                .containsOnly(OrderItemStatus.REFUND_REQUESTED);
        verify(returnRequestRepository).save(any(ReturnRequest.class));
    }

    @Test
    @DisplayName("주문이 없으면 반품 신청에 실패한다")
    void failWhenOrderNotFound() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        when(orderRepository.findByUuidWithItems(orderUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(userUuid, orderUuid,
                ReturnRequestCreateDto.builder().reason("사유").orderItemUuids(List.of(UUID.randomUUID())).build()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("구매자 본인 주문이 아니면 반품 신청에 실패한다")
    void failWhenOrderOwnerMismatch() {
        UUID userUuid = UUID.randomUUID();
        UUID orderOwnerUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = createOrder(orderUuid, orderOwnerUuid);
        when(orderRepository.findByUuidWithItems(orderUuid)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(userUuid, orderUuid,
                ReturnRequestCreateDto.builder().reason("사유").orderItemUuids(List.of(order.getOrderItems().get(0).getUuid())).build()))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    @DisplayName("반품 아이템을 선택하지 않으면 반품 신청에 실패한다")
    void failWhenReturnItemsEmpty() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = createOrder(orderUuid, userUuid);
        when(orderRepository.findByUuidWithItems(orderUuid)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(userUuid, orderUuid,
                ReturnRequestCreateDto.builder().reason("사유").orderItemUuids(List.of()).build()))
                .isInstanceOf(ReturnItemSelectionRequiredException.class);
    }

    @Test
    @DisplayName("주문에 없는 아이템을 반품 신청하면 실패한다")
    void failWhenOrderItemNotFound() {
        UUID userUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        Order order = createOrder(orderUuid, userUuid);
        when(orderRepository.findByUuidWithItems(orderUuid)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(userUuid, orderUuid,
                ReturnRequestCreateDto.builder().reason("사유").orderItemUuids(List.of(UUID.randomUUID())).build()))
                .isInstanceOf(OrderItemNotFoundException.class);
    }

    private Order createOrder(UUID orderUuid, UUID userUuid) {
        Order order = Order.builder()
                .uuid(orderUuid)
                .userUuid(userUuid)
                .totalAmount(30_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem firstItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("상품1")
                .productPrice(10_000)
                .status(OrderItemStatus.DELIVERED)
                .build();

        OrderItem secondItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("상품2")
                .productPrice(20_000)
                .status(OrderItemStatus.DELIVERED)
                .build();

        order.addOrderItem(firstItem);
        order.addOrderItem(secondItem);
        return order;
    }
}