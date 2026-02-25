package dukku.order.boundedContext.order.app;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.global.exception.ConflictException;
import dukku.common.shared.order.event.OrderItemCanceledEvent;
import dukku.common.shared.order.event.OrderProductSaleReleasedEvent;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.common.shared.order.exception.OrderItemActionNotAllowedException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.out.OrderItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateOrderItemStatusUseCaseTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UpdateOrderItemStatusUseCase useCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("owner가 아닌 사용자는 상태 변경할 수 없다")
    void failWhenNotOwner() {
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, UUID.randomUUID(), OrderItemStatus.DELIVERED);

        authenticate(UUID.randomUUID(), "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(orderItemUuid, OrderItemStatus.CONFIRMED))
                .isInstanceOf(OrderAccessDeniedException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("일반 사용자가 허용되지 않은 상태를 요청하면 실패한다")
    void failWhenUserRequestsDisallowedStatus() {
        UUID userUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, userUuid, OrderItemStatus.DELIVERED);

        authenticate(userUuid, "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(orderItemUuid, OrderItemStatus.SHIPPED))
                .isInstanceOf(OrderItemActionNotAllowedException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("배송 시작 전 CANCEL_REQUESTED는 즉시 CANCELED로 전환되고 취소 이벤트를 발행한다")
    void cancelRequestedBeforeShipmentIsAppliedAsCanceled() {
        UUID userUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, userUuid, OrderItemStatus.PAYMENT_COMPLETED);

        authenticate(userUuid, "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        useCase.execute(orderItemUuid, OrderItemStatus.CANCEL_REQUESTED);

        assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(orderItem.getOrder().getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(eventPublisher).publish(any(OrderItemCanceledEvent.class));
        verify(eventPublisher).publish(any(OrderProductSaleReleasedEvent.class));
    }

    @Test
    @DisplayName("여러 상품 중 일부만 취소되면 주문 상태는 유지된다")
    void keepOrderStatusWhenOnlySomeItemsCanceled() {
        UUID userUuid = UUID.randomUUID();
        UUID targetOrderItemUuid = UUID.randomUUID();
        OrderItem targetOrderItem = createOrderItem(targetOrderItemUuid, userUuid, OrderItemStatus.PAYMENT_COMPLETED);

        OrderItem anotherOrderItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("추가 상품")
                .productPrice(20_000)
                .status(OrderItemStatus.PAYMENT_COMPLETED)
                .build();
        targetOrderItem.getOrder().addOrderItem(anotherOrderItem);

        authenticate(userUuid, "USER");
        when(orderItemRepository.findByUuid(targetOrderItemUuid)).thenReturn(Optional.of(targetOrderItem));

        useCase.execute(targetOrderItemUuid, OrderItemStatus.CANCEL_REQUESTED);

        assertThat(targetOrderItem.getStatus()).isEqualTo(OrderItemStatus.CANCELED);
        assertThat(anotherOrderItem.getStatus()).isEqualTo(OrderItemStatus.PAYMENT_COMPLETED);
        assertThat(targetOrderItem.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        verify(eventPublisher).publish(any(OrderItemCanceledEvent.class));
        verify(eventPublisher).publish(any(OrderProductSaleReleasedEvent.class));
    }

    @Test
    @DisplayName("배송 시작 후 CANCEL_REQUESTED는 즉시 취소 시도되어 충돌 예외가 발생한다")
    void cancelRequestedAfterShipmentFailsWithConflict() {
        UUID userUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, userUuid, OrderItemStatus.SHIPPED);

        authenticate(userUuid, "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(orderItemUuid, OrderItemStatus.CANCEL_REQUESTED))
                .isInstanceOf(ConflictException.class);

        assertThat(orderItem.getStatus()).isEqualTo(OrderItemStatus.SHIPPED);
        verify(eventPublisher, never()).publish(any(OrderItemCanceledEvent.class));
        verify(eventPublisher, never()).publish(any(OrderProductSaleReleasedEvent.class));
    }

    private void authenticate(UUID userUuid, String role) {
        CustomUserDetails principal = new CustomUserDetails(userUuid, role);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private OrderItem createOrderItem(UUID orderItemUuid, UUID userUuid, OrderItemStatus currentStatus) {
        Order order = Order.builder()
                .uuid(UUID.randomUUID())
                .userUuid(userUuid)
                .status(OrderStatus.PAID)
                .totalAmount(10_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .uuid(orderItemUuid)
                .productUuid(UUID.randomUUID())
                .sellerUuid(UUID.randomUUID())
                .productName("테스트 상품")
                .productPrice(10_000)
                .status(currentStatus)
                .build();
        order.addOrderItem(orderItem);
        return orderItem;
    }
}
