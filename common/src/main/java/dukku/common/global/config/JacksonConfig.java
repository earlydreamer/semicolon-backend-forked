package dukku.common.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)  // 날짜를 ISO-8601 문자열로 출력
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); //  LLM 응답 JSON에 예상치 못한 필드가 있어도 파싱 실패 방지
    }
}
