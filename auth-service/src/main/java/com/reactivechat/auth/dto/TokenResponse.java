package com.reactivechat.auth.dto;

import java.util.Set;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String userId,
        String username,
        Set<String> roles
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                   String userId, String username, Set<String> roles) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, userId, username, roles);
    }
}
