package com.example.shortener.url;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * REST endpoints for creating, inspecting, analysing, and revoking short URLs.
 */
@RestController
@RequestMapping("/api/urls")
public class UrlController {
    private final UrlShortenerService service;

    /**
     * Supplies the URL service used by this controller.
     */
    public UrlController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping
    /**
     * Creates a short URL through the service layer.
     */
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.created(URI.create("/api/urls")).body(service.shorten(request.url(), request.expiresAt()));
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    /**
     * Revokes the selected short URL.
     */
    public void revoke(@PathVariable String code) {
        service.revoke(code);
    }

    @GetMapping("/{code}")
    /**
     * Returns lifecycle metadata for the requested short URL.
     */
    public ShortenResponse stats(@PathVariable String code) {
        return service.response(service.find(code));
    }

    @GetMapping("/{code}/analytics")
    /**
     * Returns click and lifecycle analytics for a short URL.
     */
    public Map<String, Object> analytics(@PathVariable String code) {
        ShortUrl u = service.find(code);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("code", u.getCode());
        result.put("clickCount", u.getClickCount());
        result.put("createdAt", u.getCreatedAt());
        result.put("expiresAt", u.getExpiresAt());
        result.put("status", u.getStatus());
        result.put("expiredAt", u.getExpiredAt());
        return result;
    }
}
