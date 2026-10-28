package com.example.shortener.agent;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates the model-free SDLC workflow, including parallel stages, gates, retries, and audit events.
 */
@Service
public class WorkflowOrchestrator {
    private final AiPromptService ai;
    private final Map<String, WorkflowRun> runs = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final AtomicLong ids = new AtomicLong();
    private static final Map<String, List<String>> DEPS = Map.of("requirements", List.of(), "design", List.of("requirements"), "implementation", List.of("design"), "tests", List.of("design"), "documentation", List.of("design"), "release-readiness", List.of("implementation", "tests", "documentation"));

    /**
     * Creates the orchestrator with the requirement-analysis service.
     */
    public WorkflowOrchestrator(AiPromptService ai) {
        this.ai = ai;
    }

    /**
     * Creates and executes a workflow for the requested scenario.
     */
    public WorkflowRun start(WorkflowRequest req) {
        if (!Set.of("greenfield", "brownfield", "ambiguous").contains(req.scenario()))
            throw new IllegalArgumentException("scenario must be greenfield, brownfield, or ambiguous");
        String id = "run-" + ids.incrementAndGet();
        WorkflowRun run = new WorkflowRun(id, req.scenario(), req.requirement());
        run.approvalRequired = true;
        runs.put(id, run);
        event(run, "orchestrator", "RUN_CREATED", "Scenario=" + req.scenario());
        run.context.put("requirementAnalysis", ai.normalize(req.requirement()));
        run.context.put("dependencyGraph", DEPS);
        execute(run, req.simulateTestFailure());
        return run;
    }

    /**
     * Runs dependency-ordered stages, joins parallel branches, and enforces release gates.
     */
    private void execute(WorkflowRun r, boolean failTests) {
        long start = System.nanoTime();
        try {
            stage(r, "requirements", 1, () -> "Normalized intent; assumptions and acceptance criteria recorded. " + r.context.get("requirementAnalysis"));
            stage(r, "design", 1, () -> "Design: REST API + persistence; unique code; click analytics; validation and error contract.");
            CompletableFuture<Void> impl = CompletableFuture.runAsync(() -> stage(r, "implementation", 2, () -> "Generated implementation plan: create URL entity/repository/service/controller; enforce http(s), unique codes and redirect."), pool);
            CompletableFuture<Void> tests = CompletableFuture.runAsync(() -> {
                if (failTests) stage(r, "tests", 2, () -> {
                    throw new IllegalStateException("Simulated acceptance-test failure");
                });
                else
                    stage(r, "tests", 2, () -> "Validation plan: API contract, invalid URL, unknown code, click count, concurrency and persistence checks.");
            }, pool);
            CompletableFuture<Void> docs = CompletableFuture.runAsync(() -> stage(r, "documentation", 2, () -> "README, API examples, architecture, risks, test and release checklist."), pool);
            CompletableFuture.allOf(impl, tests, docs).join();
            if ("FAILED".equals(r.stages.get("tests").status())) {
                r.status = "SAFE_STOP";
                event(r, "policy", "SAFE_STOP", "Test gate failed; release stage blocked; no release action performed.");
                return;
            }
            stage(r, "release-readiness", 2, () -> "Ready for human review only; deployment is not performed by this prototype.");
            r.status = "AWAITING_APPROVAL";
            event(r, "policy", "APPROVAL_REQUIRED", "Release readiness is high-impact and requires explicit human approval.");
        } catch (Exception e) {
            r.status = "SAFE_STOP";
            event(r, "orchestrator", "SAFE_STOP", e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            r.finishedAt = Instant.now();
            r.context.put("endToEndLatencyMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    /**
     * Executes a stage with bounded retries and records its result and audit events.
     */
    private void stage(WorkflowRun r, String name, int maxAttempts, Callable<String> work) {
        Instant began = Instant.now();
        int attempt = 0;
        String output = null, error = null;
        while (attempt < maxAttempts) {
            attempt++;
            try {
                output = work.call();
                break;
            } catch (Exception e) {
                error = e.getMessage();
                r.retries++;
                event(r, "stage:" + name, "RETRY", "attempt=" + attempt + ", error=" + error);
            }
        }
        String status = output == null ? "FAILED" : "SUCCEEDED";
        r.stages.put(name, new WorkflowRun.StageState(name, status, attempt, output, error, began, Instant.now()));
        event(r, "stage:" + name, "STAGE_" + status, output == null ? String.valueOf(error) : output);
        if (output == null)
            throw new CompletionException(new IllegalStateException(name + " failed after bounded retries: " + error));
    }

    /**
     * Returns a workflow run or fails when its identifier is unknown.
     */
    public WorkflowRun get(String id) {
        WorkflowRun r = runs.get(id);
        if (r == null) throw new NoSuchElementException("workflow run not found");
        return r;
    }

    /**
     * Records a human approval or rejection for a run awaiting review.
     */
    public WorkflowRun approve(String id, boolean approved, String reviewer, String note) {
        WorkflowRun r = get(id);
        synchronized (r) {
            if (!"AWAITING_APPROVAL".equals(r.status)) throw new IllegalStateException("run is not awaiting approval");
            r.approved = approved;
            r.status = approved ? "APPROVED_FOR_RELEASE" : "REJECTED";
            event(r, reviewer == null ? "human-reviewer" : reviewer, approved ? "APPROVED" : "REJECTED", note == null ? "No note" : note);
            return r;
        }
    }

    /**
     * Invalidates prior outputs and reruns the workflow for a changed requirement.
     */
    public WorkflowRun replan(String id, String newRequirement) {
        WorkflowRun r = get(id);
        synchronized (r) {
            if ("APPROVED_FOR_RELEASE".equals(r.status))
                throw new IllegalStateException("cannot replan an approved run");
            r.requirement = newRequirement;
            r.currentPlanVersion = "v" + (Integer.parseInt(r.currentPlanVersion.substring(1)) + 1);
            r.stages.clear();
            r.status = "REPLANNED";
            r.context.put("requirementAnalysis", ai.normalize(newRequirement));
            r.context.put("invalidatedStages", List.of("requirements", "design", "implementation", "tests", "documentation", "release-readiness"));
            event(r, "orchestrator", "REPLAN", "Upstream requirement changed; all dependent outputs invalidated; approval must be repeated.");
            r.status = "RUNNING";
            execute(r, false);
            return r;
        }
    }

    /**
     * Summarizes workflow counts, retries, latency, and recovery-related measurements.
     */
    public Map<String, Object> metrics() {
        long total = runs.size(), success = runs.values().stream().filter(r -> Set.of("AWAITING_APPROVAL", "APPROVED_FOR_RELEASE").contains(r.status)).count(), retries = runs.values().stream().mapToLong(r -> r.retries).sum(), rollbacks = runs.values().stream().mapToLong(r -> r.rollbacks).sum();
        double avg = runs.values().stream().mapToLong(r -> ((Number) r.context.getOrDefault("endToEndLatencyMs", 0L)).longValue()).average().orElse(0);
        return Map.of("runs", total, "successRate", total == 0 ? 0.0 : (double) success / total, "retryCount", retries, "rollbackCount", rollbacks, "averageEndToEndLatencyMs", avg, "mttrMs", 0, "mttrNote", "MTTR is not meaningful without incident/recovery events; zero means no recovery events measured.");
    }

    /**
     * Appends an immutable audit event to the workflow run.
     */
    private void event(WorkflowRun r, String actor, String action, String detail) {
        r.audit.add(new WorkflowRun.AuditEvent(Instant.now(), actor, action, detail));
    }
}
