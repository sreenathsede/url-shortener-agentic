package com.example.shortener.url;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_url_code", columnList = "code", unique = true),
        @Index(name = "idx_short_urls_expiry", columnList = "status,expires_at")
})
/**
 * JPA entity representing a short URL and its lifecycle metadata.
 */
public class ShortUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 16)
    private String code;
    @Column(nullable = false, length = 2048)
    private String originalUrl;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private long clickCount;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShortUrlStatus status;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "expired_at")
    private Instant expiredAt;

    /**
     * JPA-only constructor used when materializing a persisted entity.
     */
    protected ShortUrl() {
    }

    /**
     * Creates a new active short URL with its destination and expiry deadline.
     */
    public ShortUrl(String code, String originalUrl, Instant expiresAt) {
        this.code = code;
        this.originalUrl = originalUrl;
        this.createdAt = Instant.now();
        this.clickCount = 0;
        this.status = ShortUrlStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    /**
     * Returns the database identifier.
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the public short code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the destination URL.
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * Returns the creation timestamp.
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the number of successful redirects.
     */
    public long getClickCount() {
        return clickCount;
    }

    /**
     * Returns the response status.
     */
    public ShortUrlStatus getStatus() {
        return status;
    }

    /**
     * Returns the configured expiry deadline.
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Returns when the URL was marked expired.
     */
    public Instant getExpiredAt() {
        return expiredAt;
    }

    /**
     * Increments the click counter for a successful redirect.
     */
    public void recordClick() {
        clickCount++;
    }

    /**
     * Reports whether the expiry deadline has elapsed at the supplied instant.
     */
    public boolean isExpiredAt(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    /**
     * Changes an active, due URL to EXPIRED and records the transition time.
     */
    public void markExpired(Instant now) {
        if (status == ShortUrlStatus.ACTIVE && isExpiredAt(now)) {
            status = ShortUrlStatus.EXPIRED;
            expiredAt = now;
        }
    }

    /**
     * Revokes the selected short URL.
     */
    public void revoke() {
        if (status == ShortUrlStatus.ACTIVE) status = ShortUrlStatus.REVOKED;
    }
}
