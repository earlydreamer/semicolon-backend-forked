package dukku.common.shared.product.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String presignedUrl;
    private String key;
    private String publicUrl;
}
