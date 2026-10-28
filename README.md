# Agentic URL Shortener (Java / Spring Boot)

A runnable prototype for the supplied “Interview Assignment: Build an Agentic Software Engineering System — URL Shortener”. It combines a real URL-shortening API with an auditable SDLC workflow orchestrator. The orchestrator is not a claim that production code is autonomously deployed: implementation-stage output is a reviewable plan/artifact record, and release requires explicit human approval.

## Requirements
- Java 17+
- Maven 3.8+ (`mvn`)
- Network access on first build to download Maven dependencies

## Run
```bash
mvn clean test
mvn spring-boot:run
```
App: `http://localhost:8080`; health: `/actuator/health`; H2 console: `/h2-console` (JDBC URL `jdbc:h2:file:./data/shortener`, user `sa`, blank password). The H2 console is development-only and should be disabled in deployed environments.

## URL API
Create (default expiry is 30 days): `curl -i -X POST http://localhost:8080/api/urls -H 'Content-Type: application/json' -d '{"url":"https://example.com/a?x=1"}'`. Supply optional `expiresAt` as an ISO-8601 instant to override the default.
Redirect: `curl -i http://localhost:8080/r/{code}` (increments click count).
Metadata: `GET /api/urls/{code}`. Analytics: `GET /api/urls/{code}/analytics`. Revoke: `DELETE /api/urls/{code}`. Only absolute HTTP(S) URLs without embedded credentials are accepted. Codes are random 7-character base62 tokens. Expiry is persisted as `expires_at`; status is `ACTIVE`, `EXPIRED`, or `REVOKED`. Redirects synchronously enforce expiry (returning HTTP 410 Gone) and a scheduled sweep marks due rows every minute. Metadata/analytics calls expose lifecycle status. Default TTL is configurable with `SHORT_URL_DEFAULT_TTL_DAYS`. This demo does not yet provide custom aliases, rate limiting, auth, bot filtering, or per-referrer analytics.

## Agent workflow API
Start: `curl -X POST http://localhost:8080/api/agent/workflows -H 'Content-Type: application/json' -d '{"scenario":"greenfield","requirement":"Build a URL shortener with analytics and reliable redirects","simulateTestFailure":false}'`
Scenarios: `greenfield`, `brownfield`, `ambiguous`. Poll `GET /api/agent/workflows/{id}`. Approve/reject via `POST /api/agent/workflows/{id}/approval` with `{"approved":true,"reviewer":"human-reviewer","note":"Reviewed test and risk evidence"}`. Re-plan via `POST /api/agent/workflows/{id}/replan` with `{"requirement":"Updated requirement"}`. Metrics: `GET /api/agent/workflows/metrics`. Set `simulateTestFailure:true` to exercise bounded retries and safe-stop.

## No-model agentic mode (default)
No model API is required. The orchestrator uses deterministic requirement normalization, a declared dependency DAG, bounded stage retries, parallel execution for independent branches, policy gates, audit lineage, safe-stop on failed validation, and explicit human approval. This is a rule-based agentic workflow, not an LLM that invents arbitrary plans or autonomously edits source files. Its implementation stage records a reviewable implementation plan; it does not modify the repository or run arbitrary shell commands.

## Optional model connection
Offline deterministic mode is the default and is explicit in workflow context. To use an OpenAI-compatible Chat Completions endpoint, set `AI_ENABLED=true`, `AI_API_KEY`, optional `AI_BASE_URL` and `AI_MODEL`. Secrets must be environment variables, never committed. The model is used only to normalize requirements; it does not execute arbitrary tools, write files, run shell commands, or approve release. Provider failures fall back to deterministic normalization and are recorded.

## Orchestration design
See `AGENTS.md`, `docs/DESIGN.md`, `docs/SCENARIOS.md`, `docs/LIMITATIONS.md`, `docs/AI_PROMPT_RECORD.md`, `docs/AI_REVIEW.md`, `docs/AGENT_REVIEW.md`, and `docs/HUMAN_REVIEW.md`. Stage dependency graph: requirements → design → {implementation, tests, documentation} → release-readiness. Independent branches execute concurrently; a barrier synchronizes before release. Stage retries are bounded; a failed test gate stops the release stage. High-impact release readiness pauses for human approval. Re-planning increments plan version and invalidates dependent outputs. Audit events and run metrics are queryable.

## Verification status
Tests are included, but this artifact was generated in an environment without Maven installed; the build/test suite could not be executed here. Run `mvn clean test` locally and record the actual result in `docs/AI_PROMPT_RECORD.md` and `docs/HUMAN_REVIEW.md`. Do not represent the tests as passing until executed.
