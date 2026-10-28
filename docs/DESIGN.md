# Design specification

## Goals and boundaries
Implements a compact URL shortener and a workflow control plane demonstrating requirement normalization, dependency-aware execution, bounded retry, parallel branches, safe-stop, re-planning, audit, metrics, and a human approval gate. It is a prototype, not a production deployment system.

## Runtime components
- `url`: REST endpoints, service validation, JPA entity/repository, click counter.
- `agent`: workflow run model, stage scheduler, optional LLM normalizer, workflow API.
- `common`: consistent HTTP error mapping.
- H2 file DB for local persistence.

## URL API and invariants
`POST /api/urls` accepts `{ "url": "https://..." }`; returns a 7-character code, short URL, original URL and creation timestamp. `GET /r/{code}` redirects with 302 and increments clicks. `GET /api/urls/{code}` returns metadata; `/analytics` returns click count and creation time. Reject non-HTTP(S), relative URLs, missing hosts, and embedded user credentials. Unique DB constraint and pre-check protect code uniqueness; a bounded code allocation loop fails closed.

## Orchestration graph
```text
requirements -> design -> implementation --+
                       -> tests -------------+-> release-readiness -> HUMAN APPROVAL
                       -> documentation -----+
```
Requirements and design are sequential. Implementation, tests, and documentation run concurrently and synchronize at a barrier. Release readiness is never entered if the test stage fails. Each stage records start/end, attempts, status, output/error; audit events record actor/action/detail. Re-planning increments the plan version and clears all stage results because every stage is transitively dependent on the changed requirement. An already approved run cannot be re-planned.

## Governance and failure policy
- Max attempts are explicitly bounded per stage (1 or 2 depending on stage); failed work causes `SAFE_STOP`.
- A simulated test failure exercises retry and prevents release readiness.
- Release readiness only prepares a reviewable state; no deployment or destructive action is implemented.
- Approval/rejection requires a reviewer field and creates an audit event.
- Optional model access is limited to requirement normalization; output is treated as untrusted text. Model output cannot trigger tools, modify code, or bypass tests/approval. On error, use deterministic fallback.
- Metrics expose runs, success rate (workflow reaches approval state), retries, rollbacks, average workflow latency and a candid MTTR note. MTTR is not meaningful without incident/recovery events.

## Data and security
The URL entity uses a unique indexed code. Input length is capped. Redirect targets are restricted to HTTP(S) at creation, mitigating javascript/data scheme abuse and embedded credentials. There is no authentication, authorization, rate limiting, abuse detection, tenant isolation, or privacy retention policy; do not expose publicly as-is. H2 console must be disabled outside local development. API key is read from environment and never persisted in run records.

## Trade-offs
In-memory workflow runs are simple to inspect but disappear on restart and are not multi-instance safe. H2 simplifies setup but is not intended for high-throughput production. Click count updates can become a hot row. Random short codes have collision probability; DB uniqueness plus bounded retries protects correctness. Workflow execution is synchronous from the caller's perspective though branches are concurrent internally; large/long-running jobs should use durable queue/workflow infrastructure.


## URL lifecycle state
`ShortUrl` persists `status` (`ACTIVE`, `EXPIRED`, `REVOKED`), `expiresAt`, and `expiredAt`. The default expiry is configurable (30 days by default); clients may supply a future `expiresAt` on creation. A redirect enforces expiry at request time and returns `410 Gone` for expired/revoked codes. A scheduled batch sweep persists expiration independently. The request-time check is the correctness guard; the sweep is housekeeping. Analytics reports status and expiry timestamps.

## Model-free execution
The orchestrator can run with no external model. It is a deterministic, policy-driven agentic workflow: stateful DAG execution, parallel independent stages, bounded retries, validation gates, audit events, re-planning, safe-stop, and human approval. This should not be confused with an LLM-based coding agent; no source-code editing or shell execution is performed by the orchestrator.
