package dukku.order.boundedContext.order.app;

import dukku.common.shared.order.exception.ReturnApprovalAccessDeniedException;
import dukku.common.shared.order.exception.ReturnRequestNotFoundException;
import dukku.common.shared.order.exception.ReturnRequestStatusInvalidException;
import dukku.common.shared.order.type.OrderItemStatus;
import dukku.common.shared.order.type.OrderStatus;
import dukku.common.shared.order.type.ReturnStatus;
import dukku.order.boundedContext.order.entity.Order;
import dukku.order.boundedContext.order.entity.OrderItem;
import dukku.order.boundedContext.order.entity.ReturnItem;
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
class FinalRejectReturnUseCaseTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private FinalRejectReturnUseCase useCase;

    @Test
    @DisplayName("반품 발송 이후 판매자 최종 거절 시 거절 상태로 변경되고 아이템은 배송완료로 복구된다")
    void finalRejectSuccess() {
        UUID sellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(sellerUuid, ReturnStatus.RETURN_SHIPPED, OrderItemStatus.REFUND_REQUESTED);

        when(returnRequestRepository.findByUuid(returnRequest.getUuid())).thenReturn(Optional.of(returnRequest));

        useCase.execute(sellerUuid, returnRequest.getUuid(), "회수 상품 상태 불량");

        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.RETURN_REJECTED_AFTER_SHIPMENT);
        assertThat(returnRequest.getRejectionReason()).isEqualTo("회수 상품 상태 불량");
        assertThat(returnRequest.getReturnItems().get(0).getOrderItem().getStatus()).isEqualTo(OrderItemStatus.DELIVERED);
    }

    @Test
    @DisplayName("반품 요청이 없으면 판매자 최종 거절에 실패한다")
    void failWhenReturnRequestNotFound() {
        UUID returnRequestUuid = UUID.randomUUID();
        when(returnRequestRepository.findByUuid(returnRequestUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), returnRequestUuid, "사유"))
                .isInstanceOf(ReturnRequestNotFoundException.class);
    }

    @Test
    @DisplayName("반품 발송 완료 상태가 아니면 판매자 최종 거절을 할 수 없다")
    void failWhenStatusInvalid() {
        UUID sellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(sellerUuid, ReturnStatus.RETURN_REQUESTED, OrderItemStatus.REFUND_REQUESTED);

        when(returnRequestRepository.findByUuid(returnRequest.getUuid())).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> useCase.execute(sellerUuid, returnRequest.getUuid(), "사유"))
                .isInstanceOf(ReturnRequestStatusInvalidException.class);
    }

    @Test
    @DisplayName("타 판매자 요청은 판매자 최종 거절을 처리할 수 없다")
    void failWhenSellerOwnershipInvalid() {
        UUID sellerUuid = UUID.randomUUID();
        ReturnRequest returnRequest = createReturnRequest(UUID.randomUUID(), ReturnStatus.RETURN_SHIPPED, OrderItemStatus.REFUND_REQUESTED);

        when(returnRequestRepository.findByUuid(returnRequest.getUuid())).thenReturn(Optional.of(returnRequest));

        assertThatThrownBy(() -> useCase.execute(sellerUuid, returnRequest.getUuid(), "사유"))
                .isInstanceOf(ReturnApprovalAccessDeniedException.class);
    }

    private ReturnRequest createReturnRequest(UUID itemSellerUuid, ReturnStatus status, OrderItemStatus itemStatus) {
        UUID buyerUuid = UUID.randomUUID();

        Order order = Order.builder()
                .uuid(UUID.randomUUID())
                .userUuid(buyerUuid)
                .totalAmount(10_000)
                .address("서울")
                .recipient("구매자")
                .contactNumber("010-0000-0000")
                .status(OrderStatus.PAID)
                .build();

        OrderItem orderItem = OrderItem.builder()
                .uuid(UUID.randomUUID())
                .productUuid(UUID.randomUUID())
                .sellerUuid(itemSellerUuid)
                .productName("상품")
                .productPrice(10_000)
                .status(itemStatus)
                .build();
        order.addOrderItem(orderItem);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .uuid(UUID.randomUUID())
                .order(order)
                .userUuid(buyerUuid)
                .reason("사유")
                .status(status)
                .build();
        returnRequest.addReturnItem(ReturnItem.create(orderItem, 10_000));

        return returnRequest;
    }
}