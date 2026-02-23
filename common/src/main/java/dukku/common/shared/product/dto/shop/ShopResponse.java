package dukku.common.shared.product.dto.shop;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ShopResponse {
    private UUID shopUuid;              // ProductSeller.uuid
    private String nickname;
    private String intro;
    private int salesCount;
    private int activeListingCount;
    private BigDecimal averageRating;
    private int reviewCount;
}
