package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 판매자 반품 1차 거절 처리 유스케이스
 */
@Service
@RequiredArgsConstructor
public class SellerRejectReturnUseCase {

    private final ReturnRequestRepository returnRequestRepository;

    /**
     * 판매자 권한 및 상태 검증 후 반품 1차 거절 처리
     */
    @Transactional
    public ReturnResponse execute(UUID sellerUuid, UUID returnRequestUuid, String rejectionReason) {
        ReturnRequest returnRequest = returnRequestRepository.findByUuid(returnRequestUuid)
                .orElseThrow(ReturnRequestNotFoundException::new);

        validateSellerOwnership(sellerUuid, returnRequest);

        ReturnStatus status = returnRequest.getStatus();
        if (status != ReturnStatus.RETURN_REQUESTED && status != ReturnStatus.RETURN_SELLER_APPROVED) {
            throw new ReturnRequestStatusInvalidException(
                    status,
                    ReturnStatus.RETURN_REQUESTED.name() + " 또는 " + ReturnStatus.RETURN_SELLER_APPROVED.name());
        }

        returnRequest.rejectBeforeShipment(rejectionReason);

        returnRequest.getReturnItems().forEach(item -> {
            OrderItemStatus currentStatus = item.getOrderItem().getStatus();
            if (currentStatus == OrderItemStatus.REFUND_REQUESTED || currentStatus == OrderItemStatus.REFUND_IN_PROGRESS) {
                item.getOrderItem().updateOrderStatus(OrderItemStatus.DELIVERED);
            }
        });

        return returnRequest.toResponse();
    }

    /**
     * 반품 요청의 판매자 소유권 검증
     */
    private void validateSellerOwnership(UUID sellerUuid, ReturnRequest returnRequest) {
        boolean ownedBySeller = returnRequest.getReturnItems().stream()
                .allMatch(item -> sellerUuid.equals(item.getOrderItem().getSellerUuid()));

        if (!ownedBySeller) {
            throw new ReturnApprovalAccessDeniedException();
        }
    }
}
