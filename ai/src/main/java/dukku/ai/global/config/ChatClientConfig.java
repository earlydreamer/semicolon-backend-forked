package dukku.ai.global.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dukku.ai.app.MemoryExtractionUseCase;
import dukku.ai.app.MemoryRetrievalUseCase;
import dukku.ai.global.config.advisor.GuardAdvisor;
import dukku.ai.global.config.advisor.LoggingAdvisor;
import dukku.ai.global.config.advisor.MemoryExtractionAdvisor;
import dukku.ai.global.config.advisor.MemoryRetrievalAdvisor;

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
    MemoryRetrievalAdvisor memoryRetrievalAdvisor(MemoryRetrievalUseCase memoryRetrievalService) {
        return new MemoryRetrievalAdvisor(memoryRetrievalService, 110);
    }

    @Bean
    MemoryExtractionAdvisor memoryExtractionAdvisor(MemoryExtractionUseCase memoryExtractionService) {
        return new MemoryExtractionAdvisor(memoryExtractionService, 150);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder,
                          ChatMemory chatMemory,
                          VectorStore vectorStore,
                          GuardAdvisor guardAdvisor,
                          LoggingAdvisor loggingAdvisor,
                          MemoryRetrievalAdvisor memoryRetrievalAdvisor,
                          MemoryExtractionAdvisor memoryExtractionAdvisor) {
        return builder
                .defaultSystem("당신은 사용자 정보 기반 상품 추천 모델입니다")
                .defaultAdvisors(
                        guardAdvisor,                                           // 1. 입력 검증 (order=0)
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),   // 2. 대화 메모리
                        memoryRetrievalAdvisor,                                 // 3. 장기 기억 조회 (order=110)
                        QuestionAnswerAdvisor.builder(vectorStore).build(),     // 4. RAG 문서 검색
                        memoryExtractionAdvisor,                                // 5. 기억 추출 (order=150)
                        loggingAdvisor                                          // 6. 로깅/관측 (order=200)
                )
                .build();
    }
}
