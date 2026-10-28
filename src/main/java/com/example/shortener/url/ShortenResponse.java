package com.example.shortener.url;

import java.time.Instant;

/**
 * Public response representation for a shortened URL.
 */
public record ShortenResponse(String code, String shortUrl, String originalUrl, Instant createdAt,
                              Instant expiresAt, ShortUrlStatus status) {
}
