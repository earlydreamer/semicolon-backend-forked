package dukku.common.shared.ai.dto;

public record UpdateAiMemoryRequest(
        Double importanceScore,
        Double confidenceScore
) {
}
