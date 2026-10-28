package com.example.shortener.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Validated request payload for creating a short URL.
 */
public record ShortenRequest(@NotBlank @Size(max = 2048) String url, Instant expiresAt) {
}
