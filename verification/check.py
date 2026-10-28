#!/usr/bin/env python3
"""Offline source-contract checks for critical URL and workflow safety rules.

This is intentionally dependency-free. It checks the production source text and
runs seeded source mutations to prove that selected checks detect regressions.
It is NOT a substitute for compiling and running the Maven/JUnit suite.
"""
from pathlib import Path
import shutil
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/example/shortener"

CONTRACTS = [
    ("lifecycle enum includes ACTIVE/EXPIRED/REVOKED", "url/ShortUrlStatus.java", ["ACTIVE", "EXPIRED", "REVOKED"]),
    ("entity persists expiry deadline", "url/ShortUrl.java", ["expiresAt", 'name = "expires_at"']),
    ("entity persists expiry transition timestamp", "url/ShortUrl.java", ["expiredAt", 'name = "expired_at"']),
    ("entity guards expiry transition by time", "url/ShortUrl.java", ["isExpiredAt(now)"]),
    ("repository finds only active links due for expiry", "url/ShortUrlRepository.java", ["findAllByStatusAndExpiresAtLessThanEqual"]),
    ("batch expiry selects ACTIVE links using current instant", "url/UrlShortenerService.java", ["findAllByStatusAndExpiresAtLessThanEqual(ShortUrlStatus.ACTIVE, now)"]),
    ("batch expiry mutates managed entities", "url/UrlShortenerService.java", ["due.forEach(u -> u.markExpired(now))"]),
    ("scheduled expiry sweep is enabled", "url/ExpirySweep.java", ["@Scheduled(fixedDelayString", "service.markExpiredBatch()"]),
    ("redirect validates link usability before click count", "url/UrlShortenerService.java", ["enforceUsable(u);", "u.recordClick();"]),
    ("expired redirect maps to HTTP 410", "url/UrlShortenerService.java", ["HttpStatus.GONE", "short URL has expired"]),
    ("revoked redirect maps to HTTP 410", "url/UrlShortenerService.java", ["short URL has been revoked"]),
    ("expiry supplied by caller must be future", "url/UrlShortenerService.java", ["!expiry.isAfter(now)", "HttpStatus.BAD_REQUEST"]),
    ("URL validation restricts schemes to HTTP and HTTPS", "url/UrlShortenerService.java", ["equalsIgnoreCase(\"http\")", "equalsIgnoreCase(\"https\")"]),
    ("short-code allocation has a bounded retry", "url/UrlShortenerService.java", ["attempts > 8"]),
    ("workflow declares a dependency graph", "agent/WorkflowOrchestrator.java", ["DEPS=Map.of", "release-readiness"]),
    ("independent workflow branches join at a barrier", "agent/WorkflowOrchestrator.java", ["CompletableFuture.allOf(impl,tests,docs).join()"]),
    ("failed tests stop before release readiness", "agent/WorkflowOrchestrator.java", ["SAFE_STOP", "Test gate failed", '"FAILED".equals(r.stages.get("tests").status())']),
    ("stage retry loop is bounded", "agent/WorkflowOrchestrator.java", ["while(attempt<maxAttempts)"]),
    ("approval is restricted to awaiting-approval state", "agent/WorkflowOrchestrator.java", ["AWAITING_APPROVAL", "run is not awaiting approval"]),
    ("replan invalidates downstream artifacts", "agent/WorkflowOrchestrator.java", ["invalidatedStages", "currentPlanVersion"]),
    ("workflow audit events are recorded", "agent/WorkflowOrchestrator.java", ["new WorkflowRun.AuditEvent"]),
]

MUTANTS = {
    "expiry-selection": ("url/UrlShortenerService.java", "findAllByStatusAndExpiresAtLessThanEqual(ShortUrlStatus.ACTIVE, now)", "findAllByStatusAndExpiresAtLessThanEqual(ShortUrlStatus.EXPIRED, now)"),
    "expiry-transition": ("url/UrlShortenerService.java", "due.forEach(u -> u.markExpired(now))", "due.forEach(u -> u.recordClick())"),
    "redirect-guard": ("url/UrlShortenerService.java", "enforceUsable(u);", "// enforceUsable removed"),
    "future-expiry": ("url/UrlShortenerService.java", "if (!expiry.isAfter(now))", "if (expiry.isAfter(now))"),
    "retry-bound": ("agent/WorkflowOrchestrator.java", "while(attempt<maxAttempts)", "while(true)"),
    "test-gate": ("agent/WorkflowOrchestrator.java", 'if("FAILED".equals(r.stages.get("tests").status()))', 'if(false)')
}


def load_sources(base: Path):
    return {rel: (base / rel).read_text(encoding="utf-8") for _, rel, _ in CONTRACTS} | {
        rel: (base / rel).read_text(encoding="utf-8") for rel, _, _ in MUTANTS
    }


def run_contracts(base: Path, verbose=True):
    failures = []
    for label, rel, needles in CONTRACTS:
        content = (base / rel).read_text(encoding="utf-8")
        missing = [needle for needle in needles if needle not in content]
        if missing:
            failures.append((label, missing))
            if verbose:
                print(f"FAIL {label}: missing {missing}")
        elif verbose:
            print(f"PASS {label}")
    return failures


def main():
    if len(sys.argv) > 1 and sys.argv[1] not in {"--help"}:
        print("Usage: python3 verification/check.py", file=sys.stderr)
        return 2
    print("== Production source-contract checks ==")
    failures = run_contracts(ROOT / "src/main/java/com/example/shortener")
    if failures:
        print(f"{len(CONTRACTS)-len(failures)} passed, {len(failures)} failed")
        return 1
    print(f"{len(CONTRACTS)} passed, 0 failed")
    print("== Seeded mutation checks (each mutant must be detected) ==")
    caught = 0
    with tempfile.TemporaryDirectory(prefix="shortener-verification-") as tmp:
        temp_root = Path(tmp)
        shutil.copytree(SRC, temp_root / "src", dirs_exist_ok=True)
        for name, (rel, old, new) in MUTANTS.items():
            mutant_root = temp_root / name
            if mutant_root.exists():
                shutil.rmtree(mutant_root)
            shutil.copytree(SRC, mutant_root)
            target = mutant_root / rel
            source = target.read_text(encoding="utf-8")
            if old not in source:
                print(f"FAIL mutant {name}: mutation anchor not found")
                continue
            target.write_text(source.replace(old, new, 1), encoding="utf-8")
            mutant_failures = run_contracts(mutant_root, verbose=False)
            if mutant_failures:
                print(f"PASS mutant {name}: regression detected ({len(mutant_failures)} contract failure(s))")
                caught += 1
            else:
                print(f"FAIL mutant {name}: seeded regression escaped detection")
    print(f"{caught}/{len(MUTANTS)} mutants detected")
    if caught != len(MUTANTS):
        return 1
    print("NOTE: source-contract checks only; run `mvn clean verify` for Java compilation and JUnit/API tests.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
