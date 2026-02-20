package dukku.order.boundedContext.order.app;

import dukku.common.global.exception.ForbiddenException;
import dukku.common.global.exception.NotFoundException;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.dto.ReturnTrackingRegisterDto;
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
                .orElseThrow(() -> new NotFoundException("반품 요청 정보를 찾을 수 없습니다."));

        if (!returnRequest.getUserUuid().equals(userUuid)) {
            throw new ForbiddenException("본인의 반품 요청에만 운송장을 등록할 수 있습니다.");
        }

        returnRequest.updateTrackingInfo(dto.getCarrierName(), dto.getCarrierCode(), dto.getTrackingNumber());

        return returnRequest.toResponse();
    }
}
