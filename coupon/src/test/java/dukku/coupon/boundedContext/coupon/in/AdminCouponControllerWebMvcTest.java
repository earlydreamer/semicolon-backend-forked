package dukku.coupon.boundedContext.coupon.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.coupon.boundedContext.coupon.app.command.CouponFacade;
import dukku.coupon.boundedContext.coupon.app.query.CouponQueryFacade;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCouponControllerWebMvcTest {

    @Mock
    private CouponQueryFacade couponQueryFacade;

    @Mock
    private CouponFacade couponFacade;

    @InjectMocks
    private AdminCouponController adminCouponController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(adminCouponController).build();
    }

    @Test
    @DisplayName("관리자 쿠폰 지급 API는 userUuid와 couponUuid를 파사드로 전달한다")
    void issueCouponToUser() throws Exception {
        UUID couponUuid = UUID.randomUUID();
        UUID userUuid = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/coupons/{couponUuid}/issue", couponUuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new TestIssueRequest(userUuid))))
                .andExpect(status().isNoContent());

        verify(couponFacade).issueCoupon(userUuid, couponUuid);
    }

    private record TestIssueRequest(UUID userUuid) {
    }
}
