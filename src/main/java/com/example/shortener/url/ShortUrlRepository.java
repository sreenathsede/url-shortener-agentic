package com.example.shortener.url;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence queries for short URL lookup and expiry processing.
 */

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    /**
     * Finds a short URL by its public code.
     */
    Optional<ShortUrl> findByCode(String code);

    /**
     * Checks whether a generated code is already assigned.
     */
    boolean existsByCode(String code);

    /**
     * Finds links in the supplied state whose expiry deadline is at or before the cutoff.
     */
    List<ShortUrl> findAllByStatusAndExpiresAtLessThanEqual(ShortUrlStatus status, Instant cutoff);
}
