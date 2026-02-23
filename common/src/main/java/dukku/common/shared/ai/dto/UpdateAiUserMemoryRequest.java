package dukku.common.shared.ai.dto;

public record UpdateAiUserMemoryRequest(
        Double importanceScore,
        Double confidenceScore
) {
}
