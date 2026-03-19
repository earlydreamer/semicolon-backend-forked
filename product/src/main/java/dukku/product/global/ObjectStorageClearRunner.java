package dukku.product.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
@ConditionalOnProperty(name = "product.init.clear-es-on-startup", havingValue = "true")
public class ObjectStorageClearRunner implements CommandLineRunner {

    private final S3Client s3Client;

    @Value("${storage.object.bucket}")
    private String bucketName;

    @Override
    public void run(String... args) {
        log.info("[ObjectStorageClearRunner] product.init.clear-es-on-startup=true 감지. R2 버킷의 products/ 이미지를 초기화합니다.");
        try {
            String continuationToken = null;
            int total = 0;

            do {
                ListObjectsV2Request.Builder reqBuilder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix("products/");
                if (continuationToken != null) {
                    reqBuilder.continuationToken(continuationToken);
                }
                ListObjectsV2Response res = s3Client.listObjectsV2(reqBuilder.build());

                List<ObjectIdentifier> toDelete = res.contents().stream()
                        .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                        .toList();

                if (!toDelete.isEmpty()) {
                    s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(toDelete).build())
                            .build());
                    total += toDelete.size();
                }

                continuationToken = res.isTruncated() ? res.nextContinuationToken() : null;
            } while (continuationToken != null);

            log.info("[ObjectStorageClearRunner] R2 이미지 초기화 완료. 삭제된 오브젝트: {}개", total);
        } catch (Exception e) {
            log.warn("[ObjectStorageClearRunner] R2 이미지 초기화 실패: {}", e.getMessage());
        }
    }
}
