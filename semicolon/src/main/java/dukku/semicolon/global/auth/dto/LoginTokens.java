package dukku.semicolon.global.auth.dto;

public record LoginTokens(
        String accessToken,
        String refreshToken
) {}
