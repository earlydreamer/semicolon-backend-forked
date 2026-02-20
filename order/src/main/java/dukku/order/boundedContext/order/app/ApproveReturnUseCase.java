package dukku.order.boundedContext.order.app;

import dukku.common.global.eventPublisher.EventPublisher;
import dukku.common.shared.order.dto.ReturnResponse;
import dukku.common.shared.order.event.PartialRefundRequestedEvent;
import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveReturnUseCase {

    private final ReturnRequestRepository returnRequestRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ReturnResponse execute(UUID sellerUuid, UUID returnRequestUuid) {
        ReturnRequest returnRequest = returnRequestRepository.findByUuid(returnRequestUuid)
                .orElseThrow(ReturnRequestNotFoundException::new);

        validateSellerOwnership(sellerUuid, returnRequest);

        returnRequest.approve();

        List<PartialRefundRequestedEvent.RefundItemInfo> refundItems = returnRequest.getReturnItems().stream()
                .map(item -> new PartialRefundRequestedEvent.RefundItemInfo(
                        item.getOrderItem().getUuid(),
                        item.getRefundAmount()))
                .toList();

        // PG 부분 환불 트리거 이벤트 발행
        eventPublisher.publish(new PartialRefundRequestedEvent(
                returnRequest.getUuid(),
                returnRequest.getOrder().getUuid(),
                returnRequest.getUserUuid(),
                refundItems));

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
