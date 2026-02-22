package dukku.common.shared.ai.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        String conversationId,
        @NotNull UUID userUuid,
        @NotBlank @Size(max = 500) String message
) {
}
