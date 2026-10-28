package com.example.shortener.url;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Exposes the public short-code redirect endpoint.
 */
@RestController
public class RedirectController {
    private final UrlShortenerService service;

    /**
     * Supplies the URL lifecycle service used to resolve redirects.
     */
    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/r/{code}")
    /**
     * Resolves a short code and redirects to its destination.
     */
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortUrl u = service.resolve(code);
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, URI.create(u.getOriginalUrl()).toASCIIString()).build();
    }
}
