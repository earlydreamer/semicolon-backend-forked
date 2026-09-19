package dukku.ai.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class GoogleGenAiClientConfig {

    private static final int REQUEST_TIMEOUT_MILLIS = 60_000;

    @Bean
    @Primary
    Client googleGenAiClient(GoogleGenAiConnectionProperties properties) {
        return Client.builder()
                .apiKey(properties.getApiKey())
                .vertexAI(false)
                .httpOptions(HttpOptions.builder()
                        .timeout(REQUEST_TIMEOUT_MILLIS)
                        .build())
                .build();
    }
}
