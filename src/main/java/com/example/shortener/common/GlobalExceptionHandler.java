package com.example.shortener.common;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Maps common application failures to consistent HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Converts missing-resource errors to HTTP 404.
     */
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> missing(Exception e) {
        return ResponseEntity.status(404).body(Map.of("timestamp", Instant.now().toString(), "status", 404, "error", e.getMessage()));
    }

    /**
     * Converts invalid arguments to HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> bad(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now().toString(), "status", 400, "error", e.getMessage()));
    }

    /**
     * Converts state conflicts to HTTP 409.
     */
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> conflict(Exception e) {
        return ResponseEntity.status(409).body(Map.of("timestamp", Instant.now().toString(), "status", 409, "error", e.getMessage()));
    }
}
