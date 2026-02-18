package dukku.logconsumer.repository;

import dukku.logconsumer.document.LogDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface LogRepository extends MongoRepository<LogDoc, String> {

    List<LogDoc> findByTraceIdOrderByTimestampAsc(String traceId);

    Page<LogDoc> findByServiceNameAndLevel(String serviceName, String level, Pageable pageable);

    List<LogDoc> findByServiceNameAndLevelOrderByTimestampDesc(String serviceName, String level);

    Page<LogDoc> findByServiceNameAndTimestampBetween(String serviceName, Instant from, Instant to, Pageable pageable);
}
