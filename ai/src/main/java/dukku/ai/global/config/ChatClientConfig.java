package dukku.ai.global.config;

import java.util.List;

import dukku.ai.app.service.MemoryExtractionService;
import dukku.ai.app.service.MemoryRetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import dukku.ai.app.service.DocumentRetrievalService;
import dukku.ai.global.config.advisor.DocumentRetrievalAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dukku.ai.global.config.advisor.GuardAdvisor;
import dukku.ai.global.config.advisor.LoggingAdvisor;
import dukku.ai.global.config.advisor.MemoryExtractionAdvisor;
import dukku.ai.global.config.advisor.MemoryRetrievalAdvisor;
import dukku.ai.global.config.advisor.ToolAdvisor;
import dukku.ai.global.config.tool.CartHistoryTool;
import dukku.ai.global.config.tool.NotificationTool;
import dukku.ai.global.config.tool.PurchaseHistoryTool;
import dukku.ai.global.config.tool.RecommendationSaveTool;
import dukku.ai.global.config.tool.RecommendationTool;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }

    @Bean
    GuardAdvisor guardAdvisor() {
        return new GuardAdvisor(500, List.of(), 0);
    }

    @Bean
    LoggingAdvisor loggingAdvisor() {
        return new LoggingAdvisor();
    }

    @Bean
    MemoryRetrievalAdvisor memoryRetrievalAdvisor(MemoryRetrievalService memoryRetrievalService) {
        return new MemoryRetrievalAdvisor(memoryRetrievalService, 110);
    }

    @Bean
    MemoryExtractionAdvisor memoryExtractionAdvisor(MemoryExtractionService memoryExtractionService) {
        return new MemoryExtractionAdvisor(memoryExtractionService, 150);
    }

    @Bean
    ToolAdvisor toolAdvisor() {
        return new ToolAdvisor(120);
    }

    @Bean
    DocumentRetrievalAdvisor documentRetrievalAdvisor(DocumentRetrievalService documentRetrievalUseCase) {
        return new DocumentRetrievalAdvisor(documentRetrievalUseCase, 130);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          GuardAdvisor guardAdvisor,
                          LoggingAdvisor loggingAdvisor,
                          MemoryRetrievalAdvisor memoryRetrievalAdvisor,
                          MemoryExtractionAdvisor memoryExtractionAdvisor,
                          ToolAdvisor toolAdvisor,
                          DocumentRetrievalAdvisor documentRetrievalAdvisor,
                          CartHistoryTool cartHistoryTool,
                          PurchaseHistoryTool purchaseHistoryTool,
                          RecommendationTool recommendationTool,
                          RecommendationSaveTool recommendationSaveTool,
                          NotificationTool notificationTool) {
        return builder
                .defaultSystem("당신은 사용자 정보 기반 상품 추천 모델입니다")
                .defaultTools(
                        cartHistoryTool,
                        purchaseHistoryTool,
                        recommendationTool,
                        recommendationSaveTool,
                        notificationTool
                )
                .defaultAdvisors(
                        guardAdvisor,                                           // 1. 입력 검증 (order=0)
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),   // 2. 대화 메모리
                        memoryRetrievalAdvisor,                                 // 3. 장기 기억 조회 (order=110)
                        toolAdvisor,                                            // 4. Tool 컨텍스트 (order=120)
                        documentRetrievalAdvisor,                               // 5. 선택적 문서 검색 (order=130)
                        memoryExtractionAdvisor,                                // 6. 기억 추출 (order=150)
                        loggingAdvisor                                          // 7. 로깅/관측 (order=200)
                )
                .build();
    }
}
