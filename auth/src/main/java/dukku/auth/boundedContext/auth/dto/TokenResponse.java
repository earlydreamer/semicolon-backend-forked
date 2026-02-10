package dukku.semicolon.boundedContext.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
