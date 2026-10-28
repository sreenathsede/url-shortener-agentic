# Instructions for AI coding agents working in this repository

## Scope
This repository contains a Java 17 / Spring Boot URL shortener and a deterministic, model-free SDLC workflow demonstrator. Preserve the existing scope; do not add an external model requirement, credentials, or arbitrary shell execution to the default path.

## URL lifecycle invariants
1. A newly created URL has status `ACTIVE` and an expiry deadline in the future.
2. A URL is expired when `expiresAt <= now`; expiry transitions record `expiredAt`.
3. Redirect-time validation must reject `EXPIRED` and `REVOKED` links with HTTP 410 before incrementing click count.
4. The scheduled expiry sweep is maintenance, not the enforcement boundary; redirects must remain safe even between sweeps.
5. Invalid input and failed requests must not accidentally create or increment URL records.
6. Code allocation retries are bounded; never spin forever on collisions.

## Workflow invariants
1. The dependency DAG must be respected; implementation, test, and documentation branches may run concurrently only after design.
2. Release readiness must not be reached if the test stage fails.
3. Retries are bounded and recorded; unrecoverable stage failure must safe-stop.
4. High-impact release readiness requires explicit human approval.
5. Replanning invalidates prior dependent outputs and requires approval again.
6. Do not claim rollback, autonomous source editing, build execution, or a model-free LLM capability that is not implemented.

## Before reporting done
1. Run `./verification/run.sh`; quote its observed result and describe it accurately as source-contract checks.
2. Run `mvn clean verify` in a Maven-enabled environment. If unavailable, say so; never claim Java tests passed without output.
3. Review the diff and update `docs/AI_PROMPT_RECORD.md`, `docs/HUMAN_REVIEW.md`, `docs/AGENT_REVIEW.md`, and `docs/LIMITATIONS.md` where relevant.
4. Keep secrets out of source, logs, prompt records, and test fixtures.
