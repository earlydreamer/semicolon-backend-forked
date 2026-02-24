package dukku.order.boundedContext.order.in;

import dukku.order.boundedContext.order.app.FindOrderByUuidUseCase;
import dukku.order.boundedContext.order.app.UpdateOrderStatusForAdminUseCase;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.user.out.UserApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderUserAdminControllerWebMvcTest {

    @Mock
    private FindOrderByUuidUseCase findOrderByUuidUseCase;

    @Mock
    private UpdateOrderStatusForAdminUseCase updateOrderStatusForAdminUseCase;

    @Mock
    private UserApiClient userApiClient;

    @InjectMocks
    private OrderUserAdminController orderUserAdminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderUserAdminController).build();
    }

    @Test
    @DisplayName("관리자 주문 상태 변경 API는 orderUuid와 status를 유스케이스로 전달한다")
    void updateOrderStatus() throws Exception {
        UUID orderUuid = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/admin/orders/{orderUuid}/status", orderUuid)
                        .param("status", "PAID"))
                .andExpect(status().isNoContent());

        verify(updateOrderStatusForAdminUseCase).execute(orderUuid, OrderStatus.PAID);
    }

    @Test
    @DisplayName("지원하지 않는 주문 상태면 400을 반환하고 유스케이스를 호출하지 않는다")
    void updateOrderStatus_invalidStatus() throws Exception {
        UUID orderUuid = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/admin/orders/{orderUuid}/status", orderUuid)
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest());

        verify(updateOrderStatusForAdminUseCase, never()).execute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
