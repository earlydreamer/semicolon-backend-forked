package dukku.logconsumer.document;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "service_logs")
@CompoundIndex(def = "{'serviceName': 1, 'level': 1, 'timestamp': -1}")
public class LogDoc {

    @Id
    private String id;

    @Indexed
    private String traceId;

    private String spanId;

    private String userId;

    private String serviceName;

    private String level;

    private String message;

    private String loggerName;

    private String threadName;

    private String requestUri;

    private String requestMethod;

    private String clientIp;

    @Indexed(expireAfter = "30d")
    private Instant timestamp;

    private String stackTrace;

    @Builder
    public LogDoc(String traceId, String spanId, String userId, String serviceName,
                  String level, String message, String loggerName, String threadName,
                  String requestUri, String requestMethod, String clientIp,
                  Instant timestamp, String stackTrace) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.userId = userId;
        this.serviceName = serviceName;
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.threadName = threadName;
        this.requestUri = requestUri;
        this.requestMethod = requestMethod;
        this.clientIp = clientIp;
        this.timestamp = timestamp;
        this.stackTrace = stackTrace;
    }
}
