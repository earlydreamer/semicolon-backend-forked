package dukku.common.shared.ai.dto;

import dukku.common.shared.ai.type.MemorySubType;
import dukku.common.shared.ai.type.MemoryType;

import java.time.LocalDateTime;

public record AiMemoryResponse(
        Long id,
        Long userId,
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
