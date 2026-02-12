package dukku.common.shared.product.dto.review;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class SellerReviewCreateRequest {

    @NotNull
    private UUID sellerUuid;

    @NotNull
    private UUID orderItemUuid;

    @NotNull
    private UUID productUuid;

    @Min(1)
    @Max(5)
    private int rating;

    @NotBlank
    @Size(max = 1000)
    private String content;
}
