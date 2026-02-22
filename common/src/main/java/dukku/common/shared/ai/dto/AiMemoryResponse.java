package dukku.common.shared.ai.dto;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiMemoryResponse(
        Integer aiMemoryId,
        UUID userUuid,
        MemoryType memoryType,
        MemorySubType subType,
        String content,
        Double importanceScore,
        Double confidenceScore,
        Integer accessCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
