package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerReceiveReturnUseCase {

    private final ReturnRequestRepository returnRequestRepository;

    @Transactional
    public ReturnResponse execute(UUID sellerUuid, UUID returnRequestUuid) {
        ReturnRequest returnRequest = returnRequestRepository.findByUuid(returnRequestUuid)
                .orElseThrow(ReturnRequestNotFoundException::new);

        validateSellerOwnership(sellerUuid, returnRequest);

        if (returnRequest.getStatus() != ReturnStatus.RETURN_SHIPPED) {
            throw new ReturnRequestStatusInvalidException(returnRequest.getStatus(), ReturnStatus.RETURN_SHIPPED.name());
        }

        returnRequest.markReceivedBySeller();
        return returnRequest.toResponse();
    }

    private void validateSellerOwnership(UUID sellerUuid, ReturnRequest returnRequest) {
        boolean ownedBySeller = returnRequest.getReturnItems().stream()
                .allMatch(item -> sellerUuid.equals(item.getOrderItem().getSellerUuid()));

        if (!ownedBySeller) {
            throw new ReturnApprovalAccessDeniedException();
        }
    }
}
