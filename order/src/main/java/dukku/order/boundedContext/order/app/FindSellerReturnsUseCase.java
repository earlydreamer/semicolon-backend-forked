package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.dto.SellerReturnResponse;
import dukku.order.boundedContext.order.entity.ReturnRequest;
import dukku.order.boundedContext.order.out.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 판매자가 본인 상품에 접수된 반품 요청 목록을 조회하는 UseCase
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindSellerReturnsUseCase {

    private final ReturnRequestRepository returnRequestRepository;

    public List<SellerReturnResponse> execute() {
        UUID sellerUuid = UserUtil.getUserId();
        List<ReturnRequest> returns = returnRequestRepository.findAllBySellerUuid(sellerUuid);

        return returns.stream()
                .map(this::toResponse)
                .toList();
    }

    private SellerReturnResponse toResponse(ReturnRequest rr) {
        return SellerReturnResponse.builder()
                .returnRequestUuid(rr.getUuid())
                .orderUuid(rr.getOrder().getUuid())
                .status(rr.getStatus())
                .reason(rr.getReason())
                .rejectionReason(rr.getRejectionReason())
                .carrierName(rr.getCarrierName())
                .trackingNumber(rr.getTrackingNumber())
                .createdAt(rr.getCreatedAt())
                .returnItems(rr.getReturnItems().stream()
                        .map(ri -> SellerReturnResponse.ReturnItemSummary.builder()
                                .orderItemUuid(ri.getOrderItem().getUuid())
                                .productName(ri.getOrderItem().getProductName())
                                .refundAmount(ri.getRefundAmount())
                                .build())
                        .toList())
                .build();
    }
}
