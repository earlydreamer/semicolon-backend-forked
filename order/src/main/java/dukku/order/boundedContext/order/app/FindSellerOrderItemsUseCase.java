package dukku.order.boundedContext.order.app;

import dukku.common.global.UserUtil;
import dukku.common.shared.order.dto.SellerOrderItemResponse;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.out.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 판매자가 본인 판매 주문아이템 목록을 조회하는 UseCase
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindSellerOrderItemsUseCase {

    private final OrderItemRepository orderItemRepository;

    public List<SellerOrderItemResponse> execute() {
        UUID sellerUuid = UserUtil.getUserId();
        List<OrderItem> items = orderItemRepository
                .findAllBySellerUuidOrderByOrder_CreatedAtDesc(sellerUuid);

        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    private SellerOrderItemResponse toResponse(OrderItem item) {
        return SellerOrderItemResponse.builder()
                .orderItemUuid(item.getUuid())
                .orderUuid(item.getOrder().getUuid())
                .productName(item.getProductName())
                .productPrice(item.getProductPrice())
                .imageUrl(item.getImageUrl())
                .itemStatus(item.getStatus())
                .carrierName(item.getCarrierName())
                .carrierCode(item.getCarrierCode())
                .trackingNumber(item.getTrackingNumber())
                .buyerAddress(item.getOrder().getAddress())
                .recipient(item.getOrder().getRecipient())
                .contactNumber(item.getOrder().getContactNumber())
                .orderedAt(item.getOrder().getCreatedAt())
                .build();
    }
}
