package dukku.ai.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


// Spring Boot 4.1 0-M1 마일스톤 버전에서는 Jackson 자동 구성이 안됨(기존에는 spring-boot-starter-webmcv가 objectMapper 자동 등록)
@Configuration
public class JacksonConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 ISO-8601 문자열로 출력
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); //  LLM 응답 JSON에 예상치 못한 필드가 있어도 파싱 실패 방지
    }
}
