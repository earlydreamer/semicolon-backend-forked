package dukku.common.shared.ai.dto;

import java.util.UUID;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAiUserMemoryRequest(
        @NotNull UUID userUuid,
        @NotNull MemoryType memoryType,
        @NotNull MemorySubType subType,
        @NotBlank String content,
        Double importanceScore
) {
}
