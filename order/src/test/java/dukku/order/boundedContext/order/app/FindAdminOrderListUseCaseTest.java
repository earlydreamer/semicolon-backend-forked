package dukku.order.boundedContext.order.app;

import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.shared.order.dto.AdminOrderSearchCondition;
import dukku.common.shared.order.dto.OrderListResponse;
import dukku.common.shared.order.exception.OrderAdminAccessDeniedException;
import dukku.common.shared.order.type.OrderStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.out.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAdminOrderListUseCaseTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private FindAdminOrderListUseCase useCase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 권한이 없으면 관리자 주문 목록 조회에 실패한다")
    void failWhenUserIsNotAdmin() {
        authenticate(UUID.randomUUID(), "USER");

        assertThatThrownBy(() -> useCase.execute(new AdminOrderSearchCondition(null, null, null, null, null), PageRequest.of(0, 20)))
                .isInstanceOf(OrderAdminAccessDeniedException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("관리자는 조건 기반 주문 목록을 조회할 수 있다")
    void successWhenAdmin() {
        authenticate(UUID.randomUUID(), "ADMIN");

        Order order = Order.builder()
                .uuid(UUID.randomUUID())
                .userUuid(UUID.randomUUID())
                .status(OrderStatus.PAID)
                .totalAmount(10_000)
                .address("서울")
                .recipient("관리자")
                .contactNumber("010-0000-0000")
                .refundedAmount(0)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.searchForAdmin(any(AdminOrderSearchCondition.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderListResponse> result = useCase.execute(
                new AdminOrderSearchCondition(null, null, null, null, null),
                pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderUuid()).isEqualTo(order.getUuid());
        verify(orderRepository).searchForAdmin(any(AdminOrderSearchCondition.class), eq(pageable));
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
}
