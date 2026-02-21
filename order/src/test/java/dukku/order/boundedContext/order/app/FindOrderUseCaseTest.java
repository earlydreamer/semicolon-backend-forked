package dukku.order.boundedContext.order.app;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.shared.order.exception.OrderAccessDeniedException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindOrderUseCaseTest {

    @Mock
    private OrderSupport orderSupport;

    @InjectMocks
    private FindOrderUseCase useCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자도 아니고 주문 소유자도 아니면 주문 조회에 실패한다")
    void failWhenNotAdminAndNotOwner() {
        UUID loginUserUuid = UUID.randomUUID();
        UUID orderOwnerUuid = UUID.randomUUID();
        UUID orderUuid = UUID.randomUUID();

        authenticate(loginUserUuid, "USER");
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(createOrder(orderUuid, orderOwnerUuid));

        assertThatThrownBy(() -> useCase.execute(orderUuid))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    @DisplayName("관리자는 타인 주문도 조회할 수 있다")
    void successWhenAdmin() {
        UUID orderUuid = UUID.randomUUID();
        Order order = createOrder(orderUuid, UUID.randomUUID());

        authenticate(UUID.randomUUID(), "ADMIN");
        when(orderSupport.findOrderByUuidWithItems(orderUuid)).thenReturn(order);

        Order result = useCase.execute(orderUuid);

        assertThat(result).isSameAs(order);
        verify(orderSupport).findOrderByUuidWithItems(orderUuid);
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

    private Order createOrder(UUID orderUuid, UUID userUuid) {
        return Order.builder()
                .uuid(orderUuid)
                .userUuid(userUuid)
                .status(OrderStatus.PAID)
                .totalAmount(30_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .build();
    }
}
