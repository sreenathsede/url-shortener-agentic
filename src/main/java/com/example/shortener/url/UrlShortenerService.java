package com.example.shortener.url;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
/**
 * Business rules for URL validation, code generation, redirects, and expiry.
 */
public class UrlShortenerService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    private final ShortUrlRepository repository;
    private final String base;
    private final long defaultTtlDays;

    /**
 * Creates the service with persistence, public base URL, and default lifetime configuration.
 */
public UrlShortenerService(ShortUrlRepository repository,
            @Value("${app.short-url-base}") String base,
            @Value("${app.default-ttl-days:30}") long defaultTtlDays) {
        this.repository = repository;
        this.base = base.endsWith("/") ? base : base + "/";
        this.defaultTtlDays = defaultTtlDays;
    }

    @Transactional
    /**
 * Creates a short URL through the service layer.
 */
public ShortenResponse shorten(String raw) { return shorten(raw, null); }

    @Transactional
    /**
 * Creates a short URL through the service layer.
 */
public ShortenResponse shorten(String raw, Instant requestedExpiry) {
        String normalized = validate(raw);
        Instant now = Instant.now();
        Instant expiry = requestedExpiry == null ? now.plus(defaultTtlDays, ChronoUnit.DAYS) : requestedExpiry;
        if (!expiry.isAfter(now)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresAt must be in the future");
        String code;
        int attempts = 0;
        do {
            if (++attempts > 8) throw new IllegalStateException("Could not allocate unique short code");
            code = randomCode();
        } while (repository.existsByCode(code));
        return response(repository.save(new ShortUrl(code, normalized, expiry)));
    }

    /**
 * Normalizes an absolute HTTP(S) URL and rejects unsupported or unsafe input.
 */
private String validate(String raw) {
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || uri.getUserInfo() != null) throw new IllegalArgumentException();
            return uri.toASCIIString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url must be an absolute http(s) URL without embedded credentials");
        }
    }

    @Transactional
    /**
 * Finds a short URL and synchronizes its expiry state before returning it.
 */
public ShortUrl find(String code) {
        ShortUrl u = repository.findByCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short URL not found"));
        u.markExpired(Instant.now());
        return u;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    /**
 * Checks that a short URL is usable, records the redirect click, and returns it.
 */
public ShortUrl resolve(String code) {
        ShortUrl u = repository.findByCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short URL not found"));
        enforceUsable(u);
        u.recordClick();
        return u;
    }

    /**
 * Persists an elapsed expiry transition and rejects expired or revoked links.
 */
private void enforceUsable(ShortUrl u) {
        u.markExpired(Instant.now());
        if (u.getStatus() == ShortUrlStatus.EXPIRED) throw new ResponseStatusException(HttpStatus.GONE, "short URL has expired");
        if (u.getStatus() == ShortUrlStatus.REVOKED) throw new ResponseStatusException(HttpStatus.GONE, "short URL has been revoked");
    }

    @Transactional(readOnly = true)
    /**
 * Builds the API response for a persisted short URL.
 */
public ShortenResponse response(ShortUrl u) {
        return new ShortenResponse(u.getCode(), base + u.getCode(), u.getOriginalUrl(), u.getCreatedAt(), u.getExpiresAt(), u.getStatus());
    }

    @Transactional
    /**
 * Revokes the selected short URL.
 */
public void revoke(String code) {
        ShortUrl u = repository.findByCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short URL not found"));
        u.markExpired(Instant.now());
        if (u.getStatus() == ShortUrlStatus.EXPIRED) throw new ResponseStatusException(HttpStatus.GONE, "short URL has expired and cannot be revoked");
        u.revoke();
    }

    @Transactional
    /**
 * Transitions every active URL whose expiry deadline has passed.
 */
public int markExpiredBatch() {
        Instant now = Instant.now();
        var due = repository.findAllByStatusAndExpiresAtLessThanEqual(ShortUrlStatus.ACTIVE, now);
        due.forEach(u -> u.markExpired(now));
        return due.size();
    }

    /**
 * Generates a candidate short code using a cryptographically strong random source.
 */
private String randomCode() {
        StringBuilder b = new StringBuilder(7);
        for (int i = 0; i < 7; i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return b.toString();
    }
}
