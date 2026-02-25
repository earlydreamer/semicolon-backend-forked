package dukku.common.shared.ai.dto;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record HybridSearchResult(
        UUID id,
        String content,
        String metadata,
        double vectorScore,
        double keywordScore,
        double rrfScore
) {
    private static final Pattern PRODUCT_URL_PATTERN =
            Pattern.compile("\"productUrl\"\\s*:\\s*\"([^\"]+)\"");

    public String productUrl() {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        Matcher matcher = PRODUCT_URL_PATTERN.matcher(metadata);
        return matcher.find() ? matcher.group(1) : null;
    }

    public String contentWithUrl() {
        String url = productUrl();
        return url != null ? content + " (링크: " + url + ")" : content;
    }
}
