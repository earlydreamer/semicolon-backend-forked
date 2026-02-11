package dukku.common.shared.product.dto.product;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ProductListResponse {

    private List<ProductListItemResponse> items;
    private int page;
    private int size;
    private long totalCount;
    private boolean hasNext;

    public static ProductListResponse fromByQuery(Page<ProductListItemResponse> pageResult) {
        return ProductListResponse.builder()
                .items(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalCount(pageResult.getTotalElements())
                .hasNext(pageResult.hasNext())
                .build();
    }
}
