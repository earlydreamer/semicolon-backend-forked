package dukku.order.boundedContext.order.app;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.global.eventPublisher.EventPublisher;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("주문 소유자가 아닌 사용자는 주문 상품 상태를 변경할 수 없다")
    void failWhenNotOwner() {
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, UUID.randomUUID());

        authenticate(UUID.randomUUID(), "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(orderItemUuid, OrderItemStatus.CONFIRMED))
                .isInstanceOf(OrderAccessDeniedException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("주문 소유자라도 허용되지 않은 상태 변경은 실패한다")
    void failWhenUserRequestsDisallowedStatus() {
        UUID userUuid = UUID.randomUUID();
        UUID orderItemUuid = UUID.randomUUID();
        OrderItem orderItem = createOrderItem(orderItemUuid, userUuid);

        authenticate(userUuid, "USER");
        when(orderItemRepository.findByUuid(orderItemUuid)).thenReturn(Optional.of(orderItem));

        assertThatThrownBy(() -> useCase.execute(orderItemUuid, OrderItemStatus.SHIPPED))
                .isInstanceOf(OrderItemActionNotAllowedException.class);

        verifyNoInteractions(eventPublisher);
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

    private OrderItem createOrderItem(UUID orderItemUuid, UUID userUuid) {
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
                .status(OrderItemStatus.DELIVERED)
                .build();
        order.addOrderItem(orderItem);
        return orderItem;
    }
}
