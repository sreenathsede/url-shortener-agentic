#!/usr/bin/env sh
# Dependency-free verification of critical source contracts and seeded regressions.
# Requires Python 3; does not compile Java or replace the Maven/JUnit suite.
set -eu
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
exec python3 "$HERE/check.py"
