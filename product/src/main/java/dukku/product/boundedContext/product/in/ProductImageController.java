package dukku.product.boundedContext.product.in;

import dukku.product.boundedContext.product.app.usecase.product.GeneratePresignedUrlUseCase;
import dukku.common.shared.product.dto.product.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/images")
public class ProductImageController {
    private final GeneratePresignedUrlUseCase generatePresignedUrlUseCase;

    @GetMapping("/presigned-url")
    public PresignedUrlResponse getPresignedUrl(
            @RequestParam(value = "extension", defaultValue = "jpg") String extension
    ) {
        String url = generatePresignedUrlUseCase.generatePresignedUrl(extension);

        return new PresignedUrlResponse(url);
    }
}
