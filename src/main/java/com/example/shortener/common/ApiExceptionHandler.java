package com.example.shortener.common;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

/**
 * Maps API exceptions into structured HTTP error responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * Preserves the status and reason selected by the service layer.
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("timestamp", Instant.now().toString(), "status", e.getStatusCode().value(), "error", e.getReason() == null ? "Request failed" : e.getReason()));
    }

    /**
     * Returns a stable validation error for malformed request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("timestamp", Instant.now().toString(), "status", 400, "error", "url is required and must be at most 2048 characters"));
    }
}
