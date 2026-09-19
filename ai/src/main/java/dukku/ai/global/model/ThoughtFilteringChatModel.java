package dukku.ai.global.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import reactor.core.publisher.Flux;

/**
 * Gemini의 내부 thought 텍스트를 외부 대화와 메모리 경계에서 제거한다.
 * 함수 호출 메시지는 thought signature metadata를 보존해 현재 turn의 후속 요청에 전달한다.
 */
public final class ThoughtFilteringChatModel implements ChatModel {

    private static final String IS_THOUGHT = "isThought";
    private static final String THOUGHT_SIGNATURES = "thoughtSignatures";

    private final GoogleGenAiChatModel delegate;

    public ThoughtFilteringChatModel(GoogleGenAiChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return filterThoughts(delegate.call(prompt));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(prompt)
                .map(ThoughtFilteringChatModel::filterThoughts);
    }

    @Override
    public ChatOptions getOptions() {
        return delegate.getOptions();
    }

    static ChatResponse filterThoughts(ChatResponse response) {
        if (response == null) {
            return response;
        }
        if (response.getResults() == null || response.getResults().isEmpty()) {
            // Some providers finish a stream with usage/finish metadata and no candidate.
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("").build())),
                    response.getMetadata());
        }

        List<Generation> results = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int textPosition = -1;
        Map<String, Object> textProperties = Map.of();
        org.springframework.ai.chat.metadata.ChatGenerationMetadata lastMetadata = null;

        for (Generation generation : response.getResults()) {
            if (generation == null || generation.getOutput() == null) {
                continue;
            }
            if (generation.getMetadata() != null && generation.getMetadata().getFinishReason() != null) {
                lastMetadata = generation.getMetadata();
            }

            AssistantMessage output = generation.getOutput();
            if (isThought(output)) {
                // A tool call may share its part with thought text. Keep the call and opaque
                // signature metadata for the loop, but never forward the thought text.
                if (output.hasToolCalls()) {
                    AssistantMessage safeToolCall = AssistantMessage.builder()
                            .content("")
                            .toolCalls(output.getToolCalls())
                            .properties(output.getMetadata())
                            .build();
                    results.add(new Generation(safeToolCall, generation.getMetadata()));
                } else {
                    // Thought parts have no user-visible output; their metadata is merged below.
                }
                continue;
            }

            if (output.hasToolCalls() || !output.getMedia().isEmpty()) {
                // ToolCallingAdvisor needs the original AssistantMessage, including opaque signatures.
                results.add(generation);
                continue;
            }

            String part = output.getText();
            if (part == null || part.isEmpty()) {
                // Empty native parts can carry the finish reason for visible output in this response.
                continue;
            }

            if (textPosition < 0) {
                textPosition = results.size();
                textProperties = safeTextProperties(output.getMetadata());
            }
            text.append(part);
        }

        if (textPosition >= 0) {
            AssistantMessage textOutput = AssistantMessage.builder()
                    .content(text.toString())
                    .properties(textProperties)
                    .build();
            results.add(textPosition, new Generation(textOutput, lastMetadata));
        }

        if (results.isEmpty()) {
            // A response containing only private thought or usage/finish metadata has no public text.
            // Retain its finish metadata for downstream aggregation without exposing the thought.
            results.add(new Generation(AssistantMessage.builder().content("").build(), lastMetadata));
        }

        return new ChatResponse(results, response.getMetadata());
    }

    private static boolean isThought(AssistantMessage output) {
        Object value = output.getMetadata().get(IS_THOUGHT);
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static Map<String, Object> safeTextProperties(Map<String, Object> metadata) {
        if (metadata.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> safe = new HashMap<>(metadata);
        safe.remove(IS_THOUGHT);
        // Signatures are needed only on function-call messages in the active tool loop.
        safe.remove(THOUGHT_SIGNATURES);
        return Map.copyOf(safe);
    }
}
