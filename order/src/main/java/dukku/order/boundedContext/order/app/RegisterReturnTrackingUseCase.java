package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.exception.ReturnRequestAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 반품 운송장 등록 처리 유스케이스
 */
@Service
@RequiredArgsConstructor
public class RegisterReturnTrackingUseCase {

    private final ReturnRequestRepository returnRequestRepository;

    /**
     * 구매자 권한 및 상태 검증 후 반품 운송장 등록 처리
     */
    @Transactional
    public ReturnResponse execute(UUID userUuid, UUID returnRequestUuid, ReturnTrackingRegisterDto dto) {
        ReturnRequest returnRequest = returnRequestRepository.findByUuid(returnRequestUuid)
                .orElseThrow(ReturnRequestNotFoundException::new);

        if (!returnRequest.getUserUuid().equals(userUuid)) {
            throw new ReturnRequestAccessDeniedException();
        }

        if (returnRequest.getStatus() != ReturnStatus.RETURN_SELLER_APPROVED) {
            throw new ReturnRequestStatusInvalidException(returnRequest.getStatus(), ReturnStatus.RETURN_SELLER_APPROVED.name());
        }

        returnRequest.updateTrackingInfo(dto.getCarrierName(), dto.getCarrierCode(), dto.getTrackingNumber());

        return returnRequest.toResponse();
    }
}
