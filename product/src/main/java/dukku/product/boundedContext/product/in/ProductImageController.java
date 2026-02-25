package dukku.product.boundedContext.product.in;

import dukku.common.global.UserUtil;
import dukku.product.boundedContext.product.app.usecase.product.GeneratePresignedUrlUseCase;
import dukku.product.boundedContext.product.app.usecase.product.UploadImageUseCase;
import dukku.common.shared.product.dto.product.ImageUploadResponse;
import dukku.common.shared.product.dto.product.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/images")
public class ProductImageController {
    private final GeneratePresignedUrlUseCase generatePresignedUrlUseCase;
    private final UploadImageUseCase uploadImageUseCase;

    @GetMapping("/presigned-url")
    public PresignedUrlResponse getPresignedUrl(
            @RequestParam(value = "extension", defaultValue = "jpg") String extension
    ) {
        String url = generatePresignedUrlUseCase.generatePresignedUrl(extension);

        return new PresignedUrlResponse(url);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImage(
            @RequestPart("file") MultipartFile file
    ) {
        UUID userId = UserUtil.getUserId();
        String key = uploadImageUseCase.upload(file, userId);
        String imageUrl = UriComponentsBuilder.fromPath("/api/v1/products/images/public")
                .queryParam("key", key)
                .build()
                .toUriString();
        return new ImageUploadResponse(imageUrl);
    }

    @GetMapping("/public")
    public ResponseEntity<byte[]> getPublicImage(
            @RequestParam("key") String key
    ) {
        UploadImageUseCase.S3ImageObject image = uploadImageUseCase.getImage(key);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().getHeaderValue())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.bytes());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteImage(
            @RequestParam("key") String key
    ) {
        UUID userId = UserUtil.getUserId();
        uploadImageUseCase.deleteImage(key, userId);
        return ResponseEntity.noContent().build();
    }
}
