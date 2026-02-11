package dukku.common.shared.product.dto.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import dukku.common.shared.product.type.SaleStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ProductListItemResponse {
    private UUID productUuid;
    private String title;
    private Long price;
    private String thumbnailUrl;
    private SaleStatus saleStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    private int likeCount;
    private int viewCount;
    private int commentCount;
    private List<String> tagNames;
}
