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
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveReturnUseCaseTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ApproveReturnUseCase useCase;

    @Test
    @DisplayName("최종 승인 시 모든 반품 아이템의 판매자가 본인이면 승인되고 환불 이벤트가 발행된다")
    void approveWhenSellerOwnsAllItems() {
        UUID sellerUuid = UUID.randomUUID();
        UUID buyerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(buyerUuid, sellerUuid, sellerUuid);

        when(returnRequestRepository.findByUuid(returnRequest.getUuid())).thenReturn(Optional.of(returnRequest));

        useCase.execute(sellerUuid, returnRequest.getUuid());

        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.RETURN_APPROVED);

        ArgumentCaptor<PartialRefundRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(PartialRefundRequestedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        PartialRefundRequestedEvent event = eventCaptor.getValue();
        assertThat(event.orderUuid()).isEqualTo(returnRequest.getOrder().getUuid());
        assertThat(event.userUuid()).isEqualTo(returnRequest.getUserUuid());
        assertThat(event.refundItems()).hasSize(2);
        assertThat(returnRequest.getReturnItems())
                .extracting(item -> item.getOrderItem().getStatus())
                .containsOnly(OrderItemStatus.REFUND_IN_PROGRESS);
    }

    @Test
    @DisplayName("최종 승인 시 타 판매자 아이템이 포함되어 있으면 승인할 수 없다")
    void denyWhenSellerDoesNotOwnAllItems() {
        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        UUID otherSellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(buyerUuid, sellerUuid, otherSellerUuid);

        when(returnRequestRepository.findByUuid(returnRequest.getUuid())).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> useCase.execute(sellerUuid, returnRequest.getUuid()))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);

        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.RETURN_RECEIVED);
        verify(eventPublisher, never()).publish(any());
    }

    private ReturnRequest createReturnRequest(UUID buyerUuid, UUID firstSellerUuid, UUID secondSellerUuid) {
        Order order = Order.builder()
                .uuid(UUID.randomUUID())
                .userUuid(buyerUuid)
                .totalAmount(30_000)
                .address("seoul")
                .recipient("buyer")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .status(OrderStatus.PAID)
                .build();

        OrderItem firstItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(firstSellerUuid)
                .productName("item-1")
                .productPrice(10_000)
                .status(OrderItemStatus.REFUND_REQUESTED)
                .build();

        OrderItem secondItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(secondSellerUuid)
                .productName("item-2")
                .productPrice(20_000)
                .status(OrderItemStatus.REFUND_REQUESTED)
                .build();

        order.addOrderItem(firstItem);
        order.addOrderItem(secondItem);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .uuid(UUID.randomUUID())
                .order(order)
                .userUuid(buyerUuid)
                .reason("단순 변심")
                .status(ReturnStatus.RETURN_RECEIVED)
                .build();

        returnRequest.addReturnItem(ReturnItem.create(firstItem, 10_000));
        returnRequest.addReturnItem(ReturnItem.create(secondItem, 20_000));

        return returnRequest;
    }
}
