package dukku.semicolon.global.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
