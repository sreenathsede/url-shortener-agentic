# Offline verification harness

Run from the repository root:

```sh
./verification/run.sh
# or
python3 verification/check.py
```

The harness requires Python 3 only. It checks key source-level contracts for URL lifecycle, redirect safety, and workflow governance, then applies six seeded mutations to temporary copies of production source and confirms that the contract checks detect each regression.

**Scope limitation:** this is a source-contract/mutation check, not a Java runtime test. It does not compile Spring/JPA code, exercise a database, or prove concurrency behavior. Run `mvn clean verify` for the actual Java test suite. Do not describe either check as passing until its output has been observed.
