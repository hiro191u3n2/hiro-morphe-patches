# Exact rollback audit

v1.0.382 restores the v1.0.380 patch code. This audit compares a freshly rebuilt
dev2 APK with the verified v1.0.380 dev2 APK. The other v380 configurations retain
their previous validation; they are not reported as freshly rebuilt or tested.

`python3 run_completed_audits.py` runs only dev2 by default.

## DEX

Every class is canonicalized independently, including code, metadata, fields,
annotations, debug information and exception ranges. All class hashes must match
exactly: zero additions, deletions or changes are allowed. The audit additionally
requires every raw DEX payload to be byte-identical and explicitly rejects the
v381-only `ViewCountFormat$FormatterState` class.

The existing canonical inventory cache is reused only for identical complete DEX
SHA-256 hashes. Inputs are hashed again and duplicate classes are still rejected.
No hook reconstruction, normalization exception or new runtime class is permitted.

## Resources and other payloads

The complete resources.arsc must be byte-identical to v380. All twenty UI string
values must match the restored component. The previous parsed resource cache is
reused only for identical resource-table, expected-value and parser-source hashes.

Every other non-DEX ZIP payload, including AndroidManifest.xml, must be identical
with no additions or deletions. Only signing metadata is excluded. APK SHA-256
values bind the reports to the compared artifacts.

These are static rebuilt-artifact checks. They do not claim a physical-device,
emulator, scroll-performance or fresh four-configuration test.
