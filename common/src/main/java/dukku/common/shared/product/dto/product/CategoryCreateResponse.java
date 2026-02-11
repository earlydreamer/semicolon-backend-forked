package dukku.common.shared.product.dto.product;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryCreateResponse {
    private Integer id;
    private String name;
    private Integer depth;
    private Integer parentId;
}
