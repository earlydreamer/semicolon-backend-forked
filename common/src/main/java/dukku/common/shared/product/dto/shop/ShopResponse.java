package dukku.common.shared.product.dto.shop;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ShopResponse {
    private UUID shopUuid;              // ProductSeller.uuid
    private String intro;
    private int salesCount;
    private int activeListingCount;
}
