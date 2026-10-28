package com.example.shortener.url;

/**
 * Persisted lifecycle state. Expiration is also derived from expiresAt at read time.
 */
public enum ShortUrlStatus {ACTIVE, EXPIRED, REVOKED}
