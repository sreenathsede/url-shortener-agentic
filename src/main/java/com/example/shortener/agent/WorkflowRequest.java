package com.example.shortener.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated input describing a workflow scenario and requirement.
 */
public record WorkflowRequest(@NotBlank String scenario, @NotBlank @Size(max = 4000) String requirement,
                              boolean simulateTestFailure) {
}
