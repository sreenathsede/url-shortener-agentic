package com.example.shortener.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
/**
 * Periodically transitions due short URLs to the EXPIRED lifecycle state.
 */
public class ExpirySweep {
    private static final Logger log = LoggerFactory.getLogger(ExpirySweep.class);
    private final UrlShortenerService service;

    /**
     * Creates the sweep with the URL lifecycle service.
     */
    public ExpirySweep(UrlShortenerService service) {
        this.service = service;
    }

    /**
     * Background housekeeping; redirect resolution also checks expiry synchronously.
     */
    @Scheduled(fixedDelayString = "${app.expiry-sweep-ms:60000}")
    /**
     * Marks all currently due URLs as expired and logs the number changed.
     */
    public void expireDueUrls() {
        int count = service.markExpiredBatch();
        if (count > 0) log.info("Marked {} short URLs as expired", count);
    }
}
