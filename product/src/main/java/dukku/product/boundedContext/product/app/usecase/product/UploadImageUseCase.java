package dukku.product.boundedContext.product.app.usecase.product;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UploadImageUseCase {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String upload(MultipartFile file, UUID userId) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(region)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 configuration is missing");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image file is too large");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read image bytes", e);
        }

        String extension = detectExtension(bytes);
        String contentType = contentTypeFor(extension);
        String key = "products/" + userId + "/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            String awsCode = e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode();
            String reason = awsCode == null
                    ? "Failed to upload image to S3"
                    : "Failed to upload image to S3 (" + awsCode + ")";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload image to S3", e);
        }

        return key;
    }

    public S3ImageObject getImage(String key) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(region)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 configuration is missing");
        }

        if (!StringUtils.hasText(key) || !key.startsWith("products/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image key");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            var responseBytes = s3Client.getObjectAsBytes(request);
            String contentType = responseBytes.response().contentType();
            if (!StringUtils.hasText(contentType)) {
                contentType = "application/octet-stream";
            }
            return new S3ImageObject(responseBytes.asByteArray(), contentType);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found", e);
            }
            String awsCode = e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode();
            String reason = awsCode == null
                    ? "Failed to read image from S3"
                    : "Failed to read image from S3 (" + awsCode + ")";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to read image from S3", e);
        }
    }

    public void deleteImage(String key, UUID userId) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(region)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 configuration is missing");
        }

        if (!StringUtils.hasText(key) || !key.startsWith("products/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image key");
        }
        String ownerPrefix = "products/" + userId + "/";
        if (!key.startsWith(ownerPrefix)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to delete this image");
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            String awsCode = e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorCode();
            String reason = awsCode == null
                    ? "Failed to delete image from S3"
                    : "Failed to delete image from S3 (" + awsCode + ")";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason, e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to delete image from S3", e);
        }
    }

    private String detectExtension(byte[] bytes) {
        if (isJpeg(bytes)) return "jpg";
        if (isPng(bytes)) return "png";
        if (isGif(bytes)) return "gif";
        if (isWebp(bytes)) return "webp";

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format");
    }

    private String contentTypeFor(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] b) {
        return b.length >= 8
                && (b[0] & 0xFF) == 0x89
                && b[1] == 0x50
                && b[2] == 0x4E
                && b[3] == 0x47
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A;
    }

    private boolean isGif(byte[] b) {
        return b.length >= 6
                && b[0] == 0x47
                && b[1] == 0x49
                && b[2] == 0x46
                && b[3] == 0x38
                && (b[4] == 0x37 || b[4] == 0x39)
                && b[5] == 0x61;
    }

    private boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 0x52
                && b[1] == 0x49
                && b[2] == 0x46
                && b[3] == 0x46
                && b[8] == 0x57
                && b[9] == 0x45
                && b[10] == 0x42
                && b[11] == 0x50;
    }

    public record S3ImageObject(byte[] bytes, String contentType) {
    }
}
