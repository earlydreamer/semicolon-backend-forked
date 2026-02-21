package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.exception.ReturnRequestAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterReturnTrackingUseCaseTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private RegisterReturnTrackingUseCase useCase;

    @Test
    @DisplayName("판매자 1차 승인 상태에서 구매자가 운송장을 등록하면 반품 발송 상태로 전환된다")
    void registerTrackingSuccess() {
        UUID userUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();

        ReturnRequest returnRequest = createReturnRequest(returnRequestUuid, userUuid, ReturnStatus.RETURN_SELLER_APPROVED);
        when(returnRequestRepository.findByUuid(returnRequestUuid)).thenReturn(Optional.of(returnRequest));

        ReturnResponse response = useCase.execute(userUuid, returnRequestUuid,
                ReturnTrackingRegisterDto.builder()
                        .carrierName("CJ대한통운")
                        .carrierCode("04")
                        .trackingNumber("1234567890")
                        .build());

        assertThat(response.getStatus()).isEqualTo(ReturnStatus.RETURN_SHIPPED);
        assertThat(response.getTrackingNumber()).isEqualTo("1234567890");
    }

    @Test
    @DisplayName("반품 요청이 없으면 운송장 등록에 실패한다")
    void failWhenReturnRequestNotFound() {
        UUID returnRequestUuid = UUID.randomUUID();
        when(returnRequestRepository.findByUuid(returnRequestUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), returnRequestUuid,
                ReturnTrackingRegisterDto.builder().carrierName("CJ").carrierCode("04").trackingNumber("1").build()))
                .isInstanceOf(ReturnRequestNotFoundException.class);
    }

    @Test
    @DisplayName("구매자 본인이 아니면 운송장 등록에 실패한다")
    void failWhenUserMismatch() {
        UUID ownerUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();

        ReturnRequest returnRequest = createReturnRequest(returnRequestUuid, ownerUuid, ReturnStatus.RETURN_SELLER_APPROVED);
        when(returnRequestRepository.findByUuid(returnRequestUuid)).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), returnRequestUuid,
                ReturnTrackingRegisterDto.builder().carrierName("CJ").carrierCode("04").trackingNumber("1").build()))
                .isInstanceOf(ReturnRequestAccessDeniedException.class);
    }

    @Test
    @DisplayName("판매자 1차 승인이 아니면 운송장 등록에 실패한다")
    void failWhenStatusInvalid() {
        UUID userUuid = UUID.randomUUID();
        UUID returnRequestUuid = UUID.randomUUID();

        ReturnRequest returnRequest = createReturnRequest(returnRequestUuid, userUuid, ReturnStatus.RETURN_REQUESTED);
        when(returnRequestRepository.findByUuid(returnRequestUuid)).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> useCase.execute(userUuid, returnRequestUuid,
                ReturnTrackingRegisterDto.builder().carrierName("CJ").carrierCode("04").trackingNumber("1").build()))
                .isInstanceOf(ReturnRequestStatusInvalidException.class);
    }

    private ReturnRequest createReturnRequest(UUID returnRequestUuid, UUID userUuid, ReturnStatus status) {
        Order order = Order.builder()
                .uuid(UUID.randomUUID())
                .build();

        return ReturnRequest.builder()
                .uuid(returnRequestUuid)
                .order(order)
                .userUuid(userUuid)
                .status(status)
                .reason("사유")
                .build();
    }
}