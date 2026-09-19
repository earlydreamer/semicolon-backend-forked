package dukku.ai.global.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import reactor.core.publisher.Flux;

class ThoughtFilteringChatModelTest {

    @Test
    void combinesVisibleTextMergesFinishMetadataAndNeverReturnsThoughtText() {
        ChatGenerationMetadata finish = finish("STOP");
        ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
                .id("response-1")
                .model("gemini-test")
                .build();
        ChatResponse input = new ChatResponse(List.of(
                new Generation(message("private reasoning", Map.of("isThought", true)), finish),
                new Generation(message("visible ", Map.of("isThought", false)), null),
                new Generation(message("answer", Map.of()), null),
                new Generation(message("", Map.of()), finish),
                new Generation(message("", Map.of()), null)), responseMetadata);

        ChatResponse filtered = ThoughtFilteringChatModel.filterThoughts(input);

        assertThat(filtered.getResult().getOutput().getText()).isEqualTo("visible answer");
        assertThat(filtered.getResults()).hasSize(1);
        assertThat(filtered.getResult().getMetadata().getFinishReason()).isEqualTo("STOP");
        assertThat(filtered.getMetadata()).isSameAs(responseMetadata);
        assertThat(filtered.getResults()).extracting(result -> result.getOutput().getText())
                .doesNotContain("private reasoning");
    }

    @Test
    void thoughtOnlyAndFinishOnlyStreamingChunksKeepEmptyOutputsAndMetadata() {
        GoogleGenAiChatModel delegate = mock(GoogleGenAiChatModel.class);
        ChatResponse thoughtOnly = new ChatResponse(List.of(new Generation(
                message("private reasoning", Map.of("isThought", true)), finish("STOP"))));
        ChatResponse finishOnly = new ChatResponse(List.of(new Generation(message("", Map.of()), finish("STOP"))));
        when(delegate.stream(any(Prompt.class))).thenReturn(Flux.just(thoughtOnly, finishOnly));
        ThoughtFilteringChatModel model = new ThoughtFilteringChatModel(delegate);

        List<ChatResponse> chunks = model.stream(new Prompt("input")).collectList().block();

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.getResult()).isNotNull();
            assertThat(chunk.getResult().getOutput().getText()).isEmpty();
            assertThat(chunk.getResult().getMetadata().getFinishReason()).isEqualTo("STOP");
        });
        assertThat(chunks).extracting(chunk -> chunk.getResult().getOutput().getText())
                .doesNotContain("private reasoning");
    }

    @Test
    void candidateLessUsageChunkKeepsResponseMetadataAndAnEmptyGeneration() {
        ChatResponseMetadata responseMetadata = ChatResponseMetadata.builder()
                .id("usage-only")
                .model("gemini-test")
                .build();

        ChatResponse filtered = ThoughtFilteringChatModel.filterThoughts(
                new ChatResponse(List.of(), responseMetadata));

        assertThat(filtered.getResult()).isNotNull();
        assertThat(filtered.getResult().getOutput().getText()).isEmpty();
        assertThat(filtered.getMetadata()).isSameAs(responseMetadata);
    }

    @Test
    void stripsThoughtTextFromToolCallButPreservesCallAndOpaqueSignature() {
        AssistantMessage inputMessage = AssistantMessage.builder()
                .content("private reasoning")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call-1", "function", "local_echo", "{\"text\":\"ok\"}")))
                .properties(Map.of("isThought", true, "thoughtSignatures", List.of("opaque-signature")))
                .build();

        ChatResponse filtered = ThoughtFilteringChatModel.filterThoughts(
                new ChatResponse(List.of(new Generation(inputMessage, finish("STOP")))));

        AssistantMessage output = filtered.getResult().getOutput();
        assertThat(output.getText()).isEmpty();
        assertThat(output.getToolCalls()).hasSize(1);
        assertThat(output.getMetadata()).containsEntry("thoughtSignatures", List.of("opaque-signature"));
        assertThat(output.getText()).doesNotContain("private reasoning");
    }

    private static AssistantMessage message(String text, Map<String, Object> properties) {
        return AssistantMessage.builder().content(text).properties(properties).build();
    }

    private static ChatGenerationMetadata finish(String reason) {
        return ChatGenerationMetadata.builder().finishReason(reason).build();
    }
}
