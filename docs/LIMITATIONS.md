# Limitations and follow-up risks

1. **Not full autonomous code generation:** the orchestration records stage outputs/plans; it does not safely modify files, invoke a compiler itself, or run arbitrary commands. A real execution worker and sandbox would be needed to meet the strongest interpretation of end-to-end SDLC automation.
2. **Optional AI only normalizes requirements:** offline mode is deterministic and does not call a model. Model output may be incorrect or prompt-injected; it is treated as untrusted text and cannot bypass gates.
3. **Workflow state is in-memory:** restart loses history; multiple replicas do not coordinate. Use a durable workflow store/event log and optimistic locking for production.
4. **No authentication/authorization:** approval endpoint is not safe for internet exposure. Add identity, role-based reviewer authorization, CSRF considerations where relevant, and immutable audit storage.
5. **No actual deployment/rollback integration:** rollback count is a placeholder metric and no release action is performed. A real adapter must define compensating actions, idempotency, preflight checks and human confirmation.
6. **MTTR is not measured:** no incident/recovery lifecycle exists. Metric explicitly explains that zero is not a measured recovery performance.
7. **Short-code collision and abuse:** uniqueness is protected by DB constraint and bounded allocation retries, but production needs retry on unique-constraint race, rate limits, spam/phishing controls and monitoring.
8. **Analytics are minimal:** aggregate click count only; no referrer/device/geography and no deduplication. Consider privacy and retention rules before adding dimensions.
9. **Database:** H2 is for demo. Use PostgreSQL or another production database with migrations, backup/restore and tested isolation.
10. **Operational hardening:** disable H2 console, add authentication, quotas, timeouts, tracing, structured log redaction, health/readiness policy, container image scanning and secret management.
11. **Testing gap:** automated tests are included but were not executed in this environment because Maven is absent. Test claims must remain unverified until a local/CI run.
12. **No guaranteed assessment score:** aiming for 95+/100 is a target, not a substantiated result. Highest-impact gap is the absence of a sandboxed implementation worker that creates real code artifacts and invokes actual test tools under policy.


## URL expiry lifecycle
`short_urls` now stores `status`, `expires_at`, and `expired_at`. The default TTL is 30 days and can be overridden by `SHORT_URL_DEFAULT_TTL_DAYS`; a create request can provide a future ISO-8601 `expiresAt`. Redirect resolution checks expiry synchronously, marks the row expired, and returns HTTP 410. A scheduled sweep also persists expired status without waiting for a redirect. This prevents correctness from depending on the scheduler. Revoked links also return HTTP 410. Expiry does not physically delete records; retention/purge policy remains a production decision.

## No-model agentic boundary
The default execution model is deterministic and requires no model API key. It demonstrates orchestration and governance, not generative reasoning: requirement normalization and stage outputs are rule/template driven, and implementation does not edit code or execute build tools. A stronger next step is to make tool adapters execute isolated checks and write run-scoped artifacts, then attach actual command results to audit events. Do not claim autonomous code changes or model-based reasoning in this version.
