package dukku.product.boundedContext.product.entity;

import dukku.common.shared.product.type.ConditionStatus;
import dukku.common.shared.product.type.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductReleaseReservationTest {

    @Test
    @DisplayName("결제 확정 후 SOLD_OUT 상품도 동일 주문 취소 시 ON_SALE로 복구된다")
    void releaseReservationShouldReopenSoldOutForSameOrder() {
        UUID orderUuid = UUID.randomUUID();
        Product product = createProduct();

        product.reserve(orderUuid);
        product.confirmSale(orderUuid);

        assertThat(product.getSaleStatus()).isEqualTo(SaleStatus.SOLD_OUT);

        product.releaseReservation(orderUuid);

        assertThat(product.getSaleStatus()).isEqualTo(SaleStatus.ON_SALE);
        assertThat(product.getReservedOrderUuid()).isNull();
    }

    @Test
    @DisplayName("다른 주문 UUID로는 SOLD_OUT 복구되지 않는다")
    void releaseReservationShouldIgnoreDifferentOrder() {
        UUID reservedOrderUuid = UUID.randomUUID();
        Product product = createProduct();

        product.reserve(reservedOrderUuid);
        product.confirmSale(reservedOrderUuid);

        product.releaseReservation(UUID.randomUUID());

        assertThat(product.getSaleStatus()).isEqualTo(SaleStatus.SOLD_OUT);
        assertThat(product.getReservedOrderUuid()).isEqualTo(reservedOrderUuid);
    }

    private Product createProduct() {
        Category category = Category.createRoot("전자기기");
        return Product.create(
                UUID.randomUUID(),
                category,
                "테스트 상품",
                "설명",
                10_000L,
                0L,
                ConditionStatus.NO_WEAR
        );
    }
}