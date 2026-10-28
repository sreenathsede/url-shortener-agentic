package com.example.shortener.agent;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for starting, inspecting, approving, and replanning workflow runs.
 */
@RestController
@RequestMapping("/api/agent/workflows")
public class WorkflowController {
    private final WorkflowOrchestrator service;

    /**
     * Supplies the workflow orchestrator used by this controller.
     */
    public WorkflowController(WorkflowOrchestrator service) {
        this.service = service;
    }

    @PostMapping
    /**
     * Creates and executes a workflow for the requested scenario.
     */
    public WorkflowRun start(@Valid @RequestBody WorkflowRequest r) {
        return service.start(r);
    }

    @GetMapping("/{id}")
    /**
     * Returns a workflow run or fails when its identifier is unknown.
     */
    public WorkflowRun get(@PathVariable String id) {
        return service.get(id);
    } /**
     * Implements the ApprovalRequest operation for this component.
     */
    /**
     * Carries a human approval decision and its review note.
     */
    public record ApprovalRequest(boolean approved, String reviewer, String note) {
    }

    @PostMapping("/{id}/approval")
    /**
     * Implements the approval operation for this component.
     */
    public WorkflowRun approval(@PathVariable String id, @RequestBody ApprovalRequest r) {
        return service.approve(id, r.approved(), r.reviewer(), r.note());
    } /**
     * Implements the ReplanRequest operation for this component.
     */
    /**
     * Carries a revised requirement for a new workflow plan version.
     */
    public record ReplanRequest(String requirement) {
    }

    @PostMapping("/{id}/replan")
    /**
     * Invalidates prior outputs and reruns the workflow for a changed requirement.
     */
    public WorkflowRun replan(@PathVariable String id, @RequestBody ReplanRequest r) {
        return service.replan(id, r.requirement());
    }

    @GetMapping("/metrics")
    /**
     * Summarizes workflow counts, retries, latency, and recovery-related measurements.
     */
    public Map<String, Object> metrics() {
        return service.metrics();
    }
}
