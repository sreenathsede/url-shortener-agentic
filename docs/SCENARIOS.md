# Required demonstration scenarios

## 1. Greenfield
Request: `Build a URL shortener with analytics and reliable redirects.`
- Normalization: preserve user intent; record assumptions and acceptance criteria.
- Decomposition: requirements, design, implementation, tests, documentation, release readiness.
- Execution: implementation/tests/docs are concurrent after design; barrier blocks release until all finish.
- Validation: run URL service tests and orchestration tests; inspect `stages.tests` and audit.
- Governance: status becomes `AWAITING_APPROVAL`; human must approve.

## 2. Brownfield
Request: `Add click analytics to existing URL redirects without changing redirect semantics.`
- Impact analysis (prototype behavior): `RedirectController` records click; `UrlShortenerService.resolve` owns mutation; `ShortUrl.clickCount` persists the count; analytics endpoint exposes it.
- Dependency reasoning: schema/entity and transaction semantics affect redirect and analytics; tests must cover count increment and unknown code.
- Safety: stage output is a plan, not an automatic patch; tests and reviewer gate remain mandatory.

## 3. Ambiguous requirement
Request: `Support vanity aliases and make links expire when appropriate.`
- Ambiguities: who can reserve aliases; alias grammar/case rules; collision behavior; expiry default and timezone; what expired link returns; whether old links remain valid.
- Safe handling: normalized prompt should retain ambiguity rather than silently inventing decisions. Human should resolve acceptance criteria before implementation.
- Re-plan demonstration: submit a new requirement to `/replan`; version increments and all dependent stage outputs are invalidated/recomputed.

## Failure scenario
Start with `simulateTestFailure:true`. The test stage retries up to its configured bound, records retry/audit details and the run enters `SAFE_STOP`. `release-readiness` is absent. This is the negative proof for the release gate.
