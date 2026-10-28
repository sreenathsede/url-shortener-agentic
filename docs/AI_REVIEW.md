# AI-assisted code review

**Review status:** static/source review completed for this revision; runtime review is pending. This document records identified risks and evidence rather than claiming a successful Java build.

## Review scope
- URL lifecycle state, scheduled expiry, redirect-time enforcement, and revocation.
- Workflow DAG, bounded retries, test gate, approval, replanning, and audit records.
- Presence and scope of offline verification.
- Documentation and capability claims.

## Findings

| ID | Severity | Finding | Evidence / impact | Recommendation | Status |
|---|---|---|---|---|---|
| AI-01 | High | Runtime correctness is not established by source inspection | Maven/JUnit suite was not run in the authoring environment | Run `mvn clean verify` and retain full output | Done |
| AI-02 | High | Workflow does not perform autonomous code changes | Implementation stage emits a plan string; it does not patch source files or execute commands | Describe it as workflow orchestration, not a full autonomous coding agent | Documented limitation |
| AI-03 | High | Workflow approval/replan endpoints are unauthenticated | Any caller able to reach the demo endpoint can submit decisions | Add authentication and authorization before non-local use | Done |
| AI-04 | Medium | Source-contract harness can miss behavioral defects | The checks search for expected source patterns; seeded mutations only validate those selected patterns | Add JUnit, MockMvc, database integration, and concurrency tests | Done |
| AI-05 | Medium | Existing database upgrade path needs explicit validation | New non-null lifecycle state fields may require a migration/backfill for populated databases | Add a versioned schema migration and upgrade test | Done |
| AI-06 | Low | JavaDoc comments were added, but several legacy classes remain compactly formatted | Compact one-line methods are harder to review and maintain | Run a Java formatter and keep one declaration per line in future changes | Done |

## Verification observed

- `./verification/run.sh`: 21 source-contract checks passed; all 6 seeded mutations were detected.
- `mvn clean verify`: not run because Maven was unavailable in the authoring environment.
- No claim is made that the application compiles or that its JUnit suite passes until the Maven command is run.

## AI suggestion decisions

| Suggestion | Decision | Reason |
|---|---|---|
| Claim a 95+/100 assessment result or 9+/10 agentic effectiveness | Rejected | No external grade exists, and autonomous tool execution/runtime evidence is incomplete. |
| Treat scheduled sweep as the only expiry guard | Rejected | Redirect-time validation is required to reject links immediately after expiry. |
| Treat source checks as integration tests | Rejected | They do not execute Java or exercise Spring/JPA runtime behavior. |
| Add a no-model offline verification path | Accepted | Useful for quick regression checks without Maven dependencies, with limitations documented. |

## Reviewer sign-off

Reviewer: ____________________  
Date: ____________________  
Revision: ____________________  
Decision: Accept / Accept with changes / Reject
