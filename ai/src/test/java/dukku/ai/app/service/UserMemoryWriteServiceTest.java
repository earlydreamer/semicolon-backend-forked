package dukku.ai.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import dukku.ai.entity.AiUserMemory;
import dukku.ai.global.policy.AiSimilarityPolicy;
import dukku.ai.out.AiUserMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class UserMemoryWriteServiceTest {

    private static final String PROFILE = "gemini-embedding-001:1536:retrieval-document:normalization-v1";
    private static final String CONTENT = "캠핑용 경량 텐트를 선호함";

    @Test
    void repeatedDocumentExtractionUpdatesExistingMemoryWithoutAddingAnotherRow() {
        UUID userUuid = UUID.randomUUID();
        ChatModel chatModel = mock(ChatModel.class);
        GeminiEmbeddingService embeddingService = mock(GeminiEmbeddingService.class);
        AiUserMemoryRepository repository = mock(AiUserMemoryRepository.class);
        float[] documentEmbedding = new float[1536];
        documentEmbedding[0] = 1.0f;
        ChatResponse extractionResponse = response("""
                [{"memoryType":"PREFERENCE","subType":"SHOPPING","content":"%s","confidence":0.9}]
                """.formatted(CONTENT));

        when(chatModel.call(any(Prompt.class))).thenReturn(extractionResponse, extractionResponse);
        when(embeddingService.embedDocument(CONTENT)).thenReturn(documentEmbedding);
        when(embeddingService.profile()).thenReturn(PROFILE);

        List<AiUserMemory> rows = new ArrayList<>();
        when(repository.findDuplicateMemory(eq(userUuid), anyString(), eq(PROFILE), anyDouble()))
                .thenAnswer(invocation -> rows.isEmpty() ? List.of() : List.of(rows.getFirst()));
        when(repository.save(any(AiUserMemory.class))).thenAnswer(invocation -> {
            AiUserMemory memory = invocation.getArgument(0);
            if (rows.isEmpty()) {
                rows.add(memory);
            }
            return memory;
        });

        UserMemoryWriteService service = new UserMemoryWriteService(
                chatModel, embeddingService, repository, new ObjectMapper());

        service.extractAndStoreMemories(userUuid, "질문", "답변");
        service.extractAndStoreMemories(userUuid, "질문", "답변");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getContent()).isEqualTo(CONTENT);
        assertThat(rows.getFirst().getImportanceScore()).isEqualTo(0.9);
        assertThat(rows.getFirst().getEmbedding()).isSameAs(documentEmbedding);
        assertThat(rows.getFirst().getEmbeddingProfile()).isEqualTo(PROFILE);
        verify(embeddingService, times(2)).embedDocument(CONTENT);
        verify(repository, times(2)).findDuplicateMemory(
                userUuid, Arrays.toString(documentEmbedding), PROFILE,
                AiSimilarityPolicy.MEMORY_DUPLICATE_THRESHOLD);
        verify(repository, times(2)).save(any(AiUserMemory.class));
        verify(embeddingService, org.mockito.Mockito.never()).embedQuery(anyString());
    }

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
