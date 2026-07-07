package com.rohit.url_shortner.dto;

import java.time.Instant;

public record UrlStatsResponse(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount,
        boolean expired
) {
}
