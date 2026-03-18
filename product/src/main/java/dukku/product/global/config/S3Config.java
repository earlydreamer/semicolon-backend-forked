package dukku.product.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${storage.object.access-key}")
    private String accessKey;

    @Value("${storage.object.secret-key}")
    private String secretKey;

    @Value("${storage.object.region}")
    private String region;

    @Value("${storage.object.endpoint:}")
    private String endpoint;

    // Presigned URL용 공개 엔드포인트 (브라우저가 접근 가능한 URL).
    // 설정하지 않으면 endpoint를 그대로 사용합니다.
    @Value("${storage.object.presigned-url-endpoint:}")
    private String presignedUrlEndpoint;

    @Value("${storage.object.path-style-access-enabled:false}")
    private boolean pathStyleAccessEnabled;

    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration);

        String effectiveEndpoint = StringUtils.hasText(presignedUrlEndpoint) ? presignedUrlEndpoint : endpoint;
        if (StringUtils.hasText(effectiveEndpoint)) {
            builder.endpointOverride(URI.create(effectiveEndpoint));
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();

        S3Client.Builder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration);

        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
