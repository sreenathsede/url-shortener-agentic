# AI prompt and use record

**Purpose:** make AI-assisted engineering decisions reviewable. This file records the changes made for this revision; it is not a transcript of an independently verified build. The repository owner should correct any summary that does not match their own process and append future prompts/results. Do not include credentials or personal data.

## 1. Request and response summary

### P1 — Add URL expiry lifecycle
**Prompt summary:** identify how expiry is represented and enforced; add persisted lifecycle status and expiry timestamps; support a periodic sweep and immediate redirect-time enforcement; do not require an external model API.

**Response summary:** added `ACTIVE`, `EXPIRED`, and `REVOKED`; expiry fields; repository query for due active links; a scheduled sweep; redirect-time 410 behavior; analytics metadata; and revocation. Default TTL is configurable.

| Suggestion / decision | Decision | Reason / evidence |
|---|---|---|
| Depend on a hosted model for normal execution | Rejected | The user asked for a model-free implementation; deterministic orchestration is the default. |
| Use a scheduler as the only expiry enforcement | Rejected | A URL could be used between sweeps; redirect-time validation must enforce the deadline. |
| Keep lifecycle status and timestamps in the entity | Accepted | Makes lifecycle state inspectable and allows scheduled persistence for links nobody visits. |

### P2 — Align repository quality artifacts with the reference repository
**Prompt summary:** use  structure/style reference; add a `verification/` folder, document class/method behavior, and make human/AI review records evidence-oriented.

**Response summary:** added `AGENTS.md`, `verification/run.sh`, `verification/check.py`, `verification/README.md`, expanded review records, and JavaDoc comments for declared types and methods. The reference repository's approach emphasizes explicit invariants, requirement-to-test evidence, seeded-bug detection, and not claiming checks that were not run.

| Suggestion / decision | Decision | Reason / evidence |
|---|---|---|
| Present source-contract checks as equivalent to JUnit tests | Rejected | They inspect source text and cannot prove compilation or runtime behavior; README and harness state the limitation. |
| Claim the Maven suite passed | Rejected | Maven was unavailable in the authoring environment; no successful Maven output was observed. |
| Add seeded source mutations | Accepted | Confirms selected source-level guards are sensitive to six intentional regressions. |
| Claim full autonomous coding-agent execution | Rejected | The orchestrator still emits plans/status and does not edit source files or execute build tools. |

## 2. Verification evidence observed during this revision

| Command | Observed result | What it proves / does not prove |
|---|---|---|
| `./verification/run.sh` | 21 passed, 0 failed; 6/6 seeded mutations detected | Selected source-level contracts are present and six source mutations are caught. It does not compile or run Java. |
| `mvn clean verify` | Not run: Maven unavailable in the authoring environment | Java compilation, Spring context startup, database behavior, and JUnit results remain unverified here. |
| ZIP integrity check | To be recorded after packaging | Archive readability only; not application correctness. |

## 3. Remaining verification gaps

- Run `mvn clean verify` on JDK 17+ with Maven and dependency access.
- Exercise API behavior against H2, including exact expiry boundary and scheduler persistence.
- Add runtime integration tests for redirect, expiry, revocation, and analytics.
- Consider versioned schema migration for existing databases.
- Extend the workflow from plan generation to safe, sandboxed tool execution only if the assessment scope permits it.