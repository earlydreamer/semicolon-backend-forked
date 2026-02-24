package dukku.ai.app.dto;

import java.util.UUID;

public record HybridSearchResult(
        UUID id,
        String content,
        String metadata,
        double vectorScore,
        double rrfScore
) {}
