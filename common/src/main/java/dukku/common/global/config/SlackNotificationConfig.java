package dukku.common.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Slack 알림 설정
 */
@Configuration
public class SlackNotificationConfig {

    @Bean
    public RestTemplate slackRestTemplate() {
        return new RestTemplate();
    }
}
