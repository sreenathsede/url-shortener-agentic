# Agentic effectiveness review

## Assessment against the assignment

| Capability | Evidence in this repository | Review status / limitation |
|---|---|---|
| Requirement normalization | `AiPromptService` deterministic fallback and optional configured provider | Partial; no strict structured-output schema enforcement |
| Task decomposition | Explicit `DEPS` dependency graph | Present |
| Parallel branches | Implementation, tests, and documentation use `CompletableFuture`; release waits at a barrier | Present in the in-memory prototype |
| Entry/exit gates | Failed test stage causes `SAFE_STOP`; release readiness is a later stage | Present in workflow state, but stage outputs are mostly plans |
| Bounded retry | Stage retry loop has a maximum-attempt bound | Present; runtime tests still need to be run |
| Human approval | Approval endpoint accepts decision only in `AWAITING_APPROVAL` | Present; API is not authenticated |
| Replanning | Plan version increments and dependent stages are marked invalidated | Present; replay is simplified and not a full dependency-aware execution engine |
| Audit / lineage | Per-run stage results, context and audit events | Partial; process-local, not durable, and no artifact hashes |
| Metrics | Run count, success rate, retries, rollback count and latency | Partial; rollback is not implemented and MTTR is not meaningfully measured |
| URL expiry lifecycle | Persisted status/deadline; immediate redirect guard; scheduled sweep | Implemented in source; Maven/runtime verification pending |
| Offline verification | 21 source-contract checks and six seeded source mutations | Passed when run during this revision; not a Java runtime test |
| Autonomous repository editing | None | Not implemented; do not claim full autonomous coding-agent capability |

## Honest effectiveness assessment

This is a policy-driven orchestration prototype with explicit state, a dependency graph, bounded retries, a test gate, audit events, re-planning, and human approval. It is **not** an autonomous coding agent that edits the project, invokes a build sandbox, and validates its own patch. A numeric score such as 9+/10 is not substantiated by the current implementation or verification evidence.

## Recommended next steps

1. Run the full Maven/JUnit/API suite and retain observed logs.
2. Add runtime integration tests for expiry, revocation, scheduled transitions, and click-count invariants.
3. Implement allow-listed sandbox tools (read files, produce bounded patch, run fixed verification commands) if permitted by the assignment.
4. Store tool calls, outputs, artifact hashes, approvals, and decisions in durable append-only lineage.
5. Add authenticated reviewer identity and separation of duties.
6. Implement real rollback/snapshot behavior before reporting rollback metrics.
7. Run greenfield, brownfield, ambiguous, provider-failure, and test-failure scenarios in CI.
