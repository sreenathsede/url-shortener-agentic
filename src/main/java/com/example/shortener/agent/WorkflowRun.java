package com.example.shortener.agent;

import java.time.Instant;
import java.util.*;
/**
 * Mutable state, context, stage results, and audit events for one workflow execution.
 */

/**
 * Stores mutable execution state, stage results, context, and audit lineage for one workflow run.
 */
public class WorkflowRun {
    public String id;
    public String scenario;
    public String requirement;
    public String status = "RUNNING";
    public String currentPlanVersion = "v1";
    public boolean approvalRequired;
    public boolean approved;
    public Instant startedAt = Instant.now();
    public Instant finishedAt;
    public long retries;
    public long rollbacks;
    public final Map<String, StageState> stages = new LinkedHashMap<>();
    public final List<AuditEvent> audit = new ArrayList<>();
    public final Map<String, Object> context = new LinkedHashMap<>();

    /**
     * Creates the initial state for a workflow execution.
     */
    public WorkflowRun(String id, String scenario, String requirement) {
        this.id = id;
        this.scenario = scenario;
        this.requirement = requirement;
    } /**
     * Implements the StageState operation for this component.
     */
    /**
     * Captures the outcome and timing of one workflow stage.
     */
    public record StageState(String stage, String status, int attempts, String output, String error, Instant startedAt,
                             Instant finishedAt) {
    } /**
     * Implements the AuditEvent operation for this component.
     */
    /**
     * Captures one actor action in the workflow audit trail.
     */
    public record AuditEvent(Instant at, String actor, String action, String detail) {
    }
}
