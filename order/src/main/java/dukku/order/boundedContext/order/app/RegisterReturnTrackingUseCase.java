package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
import dukku.common.shared.order.exception.ReturnRequestAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterReturnTrackingUseCase {

    private final ReturnRequestRepository returnRequestRepository;

    @Transactional
    public ReturnResponse execute(UUID userUuid, UUID returnRequestUuid, ReturnTrackingRegisterDto dto) {
        ReturnRequest returnRequest = returnRequestRepository.findByUuid(returnRequestUuid)
                .orElseThrow(ReturnRequestNotFoundException::new);

        if (!returnRequest.getUserUuid().equals(userUuid)) {
            throw new ReturnRequestAccessDeniedException();
        }

        returnRequest.updateTrackingInfo(dto.getCarrierName(), dto.getCarrierCode(), dto.getTrackingNumber());

        return returnRequest.toResponse();
    }
}
