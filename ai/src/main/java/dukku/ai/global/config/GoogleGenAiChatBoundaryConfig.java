package dukku.ai.global.config;

import dukku.ai.global.model.ThoughtFilteringChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class GoogleGenAiChatBoundaryConfig {

    @Bean
    @Primary
    ChatModel thoughtFilteringChatModel(GoogleGenAiChatModel googleGenAiChatModel) {
        return new ThoughtFilteringChatModel(googleGenAiChatModel);
    }
}
