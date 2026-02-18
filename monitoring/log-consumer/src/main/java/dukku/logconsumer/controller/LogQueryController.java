package dukku.logconsumer.controller;

import dukku.logconsumer.document.LogDoc;
import dukku.logconsumer.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogQueryController {

    private final LogRepository logRepository;
    private final MongoTemplate mongoTemplate;

    @GetMapping("/trace/{traceId}")
    public List<LogDoc> getByTraceId(@PathVariable String traceId) {
        return logRepository.findByTraceIdOrderByTimestampAsc(traceId);
    }

    @GetMapping("/errors/{serviceName}")
    public Page<LogDoc> getErrors(
            @PathVariable String serviceName,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return logRepository.findByServiceNameAndLevel(serviceName, "ERROR", pageable);
    }

    @GetMapping
    public Page<LogDoc> getLogs(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String level,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        Query query = new Query().with(pageable);

        if (serviceName != null) {
            query.addCriteria(Criteria.where("serviceName").is(serviceName));
        }
        if (level != null) {
            query.addCriteria(Criteria.where("level").is(level));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), LogDoc.class);
        List<LogDoc> content = mongoTemplate.find(query, LogDoc.class);

        return new PageImpl<>(content, pageable, total);
    }
}
