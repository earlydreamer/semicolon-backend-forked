package dukku.common.shared.ai.dto;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAiMemoryRequest(
        @NotNull Long userId,
        @NotNull MemoryType memoryType,
        @NotNull MemorySubType subType,
        @NotBlank String content,
        Double importanceScore,
        Double confidenceScore
) {
}
