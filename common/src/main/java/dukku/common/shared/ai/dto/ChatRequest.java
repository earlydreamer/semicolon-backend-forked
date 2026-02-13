package dukku.common.shared.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        String conversationId,
        @NotNull Long userId,
        @NotBlank @Size(max = 500) String message
) {
}
