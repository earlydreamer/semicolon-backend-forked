package dukku.order.boundedContext.order.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.common.global.auth.detail.CustomUserDetails;
import dukku.common.shared.order.dto.ReturnRejectDto;
import dukku.common.shared.order.dto.ReturnRequestCreateDto;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.app.ApproveReturnUseCase;
import dukku.order.boundedContext.order.app.FinalRejectReturnUseCase;
import dukku.order.boundedContext.order.app.RegisterReturnTrackingUseCase;
import dukku.order.boundedContext.order.app.RequestReturnUseCase;
import dukku.order.boundedContext.order.app.SellerApproveReturnUseCase;
import dukku.order.boundedContext.order.app.SellerReceiveReturnUseCase;
import dukku.order.boundedContext.order.app.SellerRejectReturnUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReturnControllerWebMvcTest {

    @Mock
    private RequestReturnUseCase requestReturnUseCase;

    @Mock
    private RegisterReturnTrackingUseCase registerReturnTrackingUseCase;

    @Mock
    private SellerApproveReturnUseCase sellerApproveReturnUseCase;

    @Mock
    private SellerReceiveReturnUseCase sellerReceiveReturnUseCase;

    @Mock
    private SellerRejectReturnUseCase sellerRejectReturnUseCase;

    @Mock
    private ApproveReturnUseCase approveReturnUseCase;

    @Mock
    private FinalRejectReturnUseCase finalRejectReturnUseCase;

    @InjectMocks
    private ReturnController returnController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID userUuid;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(returnController).build();

        userUuid = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(userUuid, "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("반품 신청 API는 구매자 UUID와 요청 바디를 유스케이스로 전달한다")
    void requestReturnApi() throws Exception {
        UUID orderUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();

        when(requestReturnUseCase.execute(eq(userUuid), eq(orderUuid), any(ReturnRequestCreateDto.class)))
                .thenReturn(response(returnRequestUuid, orderUuid, ReturnStatus.RETURN_REQUESTED));

        mockMvc.perform(post("/api/v1/returns/orders/{orderUuid}", orderUuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ReturnRequestCreateDto.builder()
                                .reason("상품 하자")
                                .orderItemUuids(List.of(UUID.randomUUID()))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnRequestUuid").value(returnRequestUuid.toString()))
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_REQUESTED.name()));

        verify(requestReturnUseCase).execute(eq(userUuid), eq(orderUuid), any(ReturnRequestCreateDto.class));
    }

    @Test
    @DisplayName("판매자 1차 승인 API는 판매자 UUID를 유스케이스로 전달한다")
    void sellerApproveApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();

        when(sellerApproveReturnUseCase.execute(eq(userUuid), eq(returnRequestUuid)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_SELLER_APPROVED));

        mockMvc.perform(post("/api/v1/returns/{returnRequestUuid}/seller-approve", returnRequestUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_SELLER_APPROVED.name()));

        verify(sellerApproveReturnUseCase).execute(eq(userUuid), eq(returnRequestUuid));
    }

    @Test
    @DisplayName("판매자 1차 거절 API는 거절 사유를 포함해 유스케이스로 전달한다")
    void sellerRejectApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();
        String rejectReason = "사유 불충분";

        when(sellerRejectReturnUseCase.execute(eq(userUuid), eq(returnRequestUuid), eq(rejectReason)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_REJECTED_BEFORE_SHIPMENT));

        mockMvc.perform(post("/api/v1/returns/{returnRequestUuid}/seller-reject", returnRequestUuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ReturnRejectDto.builder().reason(rejectReason).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_REJECTED_BEFORE_SHIPMENT.name()));

        verify(sellerRejectReturnUseCase).execute(eq(userUuid), eq(returnRequestUuid), eq(rejectReason));
    }

    @Test
    @DisplayName("운송장 등록 API는 구매자 UUID와 운송장 정보를 유스케이스로 전달한다")
    void registerTrackingApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();

        when(registerReturnTrackingUseCase.execute(eq(userUuid), eq(returnRequestUuid), any(ReturnTrackingRegisterDto.class)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_SHIPPED));

        mockMvc.perform(put("/api/v1/returns/{returnRequestUuid}/tracking", returnRequestUuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ReturnTrackingRegisterDto.builder()
                                .carrierName("CJ대한통운")
                                .carrierCode("04")
                                .trackingNumber("1234567890")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_SHIPPED.name()));

        verify(registerReturnTrackingUseCase).execute(eq(userUuid), eq(returnRequestUuid), any(ReturnTrackingRegisterDto.class));
    }

    @Test
    @DisplayName("판매자 수령 확인 API는 판매자 UUID를 유즈케이스로 전달한다")
    void sellerReceiveApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();

        when(sellerReceiveReturnUseCase.execute(eq(userUuid), eq(returnRequestUuid)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_RECEIVED));

        mockMvc.perform(post("/api/v1/returns/{returnRequestUuid}/seller-receive", returnRequestUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_RECEIVED.name()));

        verify(sellerReceiveReturnUseCase).execute(eq(userUuid), eq(returnRequestUuid));
    }

    @Test
    @DisplayName("최종 승인 API는 판매자 UUID를 유스케이스로 전달한다")
    void finalApproveApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();

        when(approveReturnUseCase.execute(eq(userUuid), eq(returnRequestUuid)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_APPROVED));

        mockMvc.perform(post("/api/v1/returns/{returnRequestUuid}/final-approve", returnRequestUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_APPROVED.name()));

        verify(approveReturnUseCase).execute(eq(userUuid), eq(returnRequestUuid));
    }

    @Test
    @DisplayName("최종 거절 API는 거절 사유를 포함해 유스케이스로 전달한다")
    void finalRejectApi() throws Exception {
        UUID returnRequestUuid = UUID.randomUUID();
        String rejectReason = "회수 상품 상태 불량";

        when(finalRejectReturnUseCase.execute(eq(userUuid), eq(returnRequestUuid), eq(rejectReason)))
                .thenReturn(response(returnRequestUuid, UUID.randomUUID(), ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT));

        mockMvc.perform(post("/api/v1/returns/{returnRequestUuid}/final-reject", returnRequestUuid)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(ReturnRejectDto.builder().reason(rejectReason).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT.name()));

        verify(finalRejectReturnUseCase).execute(eq(userUuid), eq(returnRequestUuid), eq(rejectReason));
    }


    private ReturnResponse response(UUID returnRequestUuid, UUID orderUuid, ReturnStatus status) {
        return ReturnResponse.builder()
                .returnRequestUuid(returnRequestUuid)
                .orderUuid(orderUuid)
                .status(status)
                .build();
    }
}
