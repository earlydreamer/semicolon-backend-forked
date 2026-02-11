package dukku.product.boundedContext.product.app.facade;

import dukku.common.global.UserUtil;
import dukku.common.shared.product.type.SaleStatus;
import dukku.product.boundedContext.product.app.usecase.shop.*;
import dukku.common.shared.product.dto.shop.ShopProductListResponse;
import dukku.common.shared.product.dto.shop.ShopResponse;
import dukku.common.shared.product.dto.shop.UpdateShopRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShopFacade {

    private final FindMyShopUseCase findMyShopUseCase;
    private final UpdateMyShopUseCase updateMyShopUseCase;
    private final FindShopUseCase findShopUseCase;
    private final FindShopProductsUseCase findShopProductsUseCase;
    private final FindMyShopProductsUseCase findMyShopProductsUseCase;

    public ShopResponse findMyShop() {
        return findMyShopUseCase.execute(UserUtil.getUserId());
    }

    public ShopProductListResponse findMyShopProducts(SaleStatus saleStatus, int page, int size) {
        return findMyShopProductsUseCase.execute(UserUtil.getUserId(), saleStatus, page, size);
    }

    public ShopResponse updateMyShop(UpdateShopRequest request) {
        return updateMyShopUseCase.execute(UserUtil.getUserId(), request);
    }

    public ShopResponse findShop(UUID shopUuid) {
        return findShopUseCase.execute(shopUuid);
    }

    public ShopProductListResponse findShopProducts(UUID shopUuid, SaleStatus saleStatus, int page, int size) {
        return findShopProductsUseCase.execute(shopUuid, saleStatus, page, size);
    }
}
