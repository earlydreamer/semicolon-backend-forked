package dukku.order.boundedContext.order.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.common.global.handler.GlobalExceptionHandler;
import dukku.order.boundedContext.order.app.OrderFacade;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerWebMvcTest {

    @Mock
    private OrderFacade orderFacade;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("주문 생성 시 sellerUuid 누락이면 400을 반환하고 유스케이스를 호출하지 않는다")
    void createOrder_withoutSellerUuid_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "address": "서울 강북구 4.19로12길 8 1",
                  "recipient": "테스터",
                  "contactNumber": "010-1234-5678",
                  "items": [
                    {
                      "productUuid": "%s",
                      "productName": "맥북 프로 14인치 M3 Pro 18GB",
                      "productPrice": 2800000,
                      "imageUrl": "https://images.unsplash.com/photo-1517336714731-489689fd1ca8"
                    }
                  ]
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(orderFacade, never()).createOrder(any());
    }
}
