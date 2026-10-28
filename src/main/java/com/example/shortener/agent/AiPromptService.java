package com.example.shortener.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
/**
 * Optional OpenAI-compatible requirement normalizer. Disabled by default; deterministic rules keep local demos runnable.
 */

/**
 * Normalizes requirements deterministically unless an optional model provider is explicitly enabled.
 */
@Service
public class AiPromptService {
    private final boolean enabled;
    private final String key, model;
    private final RestClient client;

    /**
     * Configures deterministic fallback behavior and an optional OpenAI-compatible client.
     */
    public AiPromptService(@Value("${app.ai.enabled:false}") boolean enabled, @Value("${app.ai.api-key:}") String key, @Value("${app.ai.base-url}") String base, @Value("${app.ai.model}") String model) {
        this.enabled = enabled && !key.isBlank();
        this.key = key;
        this.model = model;
        this.client = RestClient.builder().baseUrl(base).build();
    }

    /**
     * Normalizes a requirement into deterministic analysis notes.
     */
    public Map<String, Object> normalize(String requirement) {
        if (!enabled)
            return Map.of("provider", "deterministic-fallback", "normalizedRequirement", requirement.trim(), "note", "AI disabled or API key absent; no external model was called");
        try {
            Map<String, Object> body = Map.of("model", model, "temperature", 0, "messages", List.of(Map.of("role", "system", "content", "Normalize software requirements. Return concise JSON with intent, assumptions, risks, acceptanceCriteria. Do not invent facts; list ambiguity explicitly."), Map.of("role", "user", "content", requirement)));
            Map response = client.post().uri("/chat/completions").header("Authorization", "Bearer " + key).body(body).retrieve().body(Map.class);
            Object choices = response.get("choices");
            if (choices instanceof List<?> list && !list.isEmpty()) {
                Object message = ((Map<?, ?>) list.get(0)).get("message");
                return Map.of("provider", "openai-compatible", "normalizedRequirement", String.valueOf(((Map<?, ?>) message).get("content")), "model", model);
            }
            throw new IllegalStateException("Unexpected model response");
        } catch (Exception e) {
            return Map.of("provider", "deterministic-fallback", "normalizedRequirement", requirement.trim(), "note", "Model call failed; safe fallback used: " + e.getClass().getSimpleName());
        }
    }
}
