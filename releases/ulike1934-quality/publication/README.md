# ULike 1.9.34 quality publication

The user selected Q1, Q4, Q6, Q7, Q11 and Q13 from the preceding quality list.
Both standalone and integrated MPPs must pass validation before publication.
The publisher retains exact-byte checks, concurrent-update protection and atomic
main/dev Manager-feed updates. The new changelog scope is ULike only.

## Pinned inputs

| Input | Bytes | SHA-256 |
| --- | ---: | --- |
| ULike 1.9.33 | 761986 | 1a7c03aca7f9260b22f4a80cf7d17dde44408ec52e5237cf1b2633c1473d10ce |
| Bundle 1.0.166 | 17409037 | c541826f308e93b269b70c459e31406ff13328c018f42ccee0fc9056cfd30eee |

Immutable download URLs use payload commit
`255dd6d1339a76212727ef1aa9fcab88077d967e`.
If the active bundle advances, use its exact bytes as the new baseline and
increment its version once. Preserve all other application patches.

Use Temurin 21.0.8+9 (setup-java spelling `21.0.8+9.0.LTS`) and the three JARs
whose hashes are pinned in the manifest. The verified tool source is Actions run
`37577403294`, artifact `ulike1932-quality-verified-publication`. CI verifies every
JAR digest before use and archives the same toolchain again.

The previous original-APK capture-hook source remains as historical build
provenance. This release transforms the already present reviewed methods and
does not claim to extract or use a new native seed.

## Required sequence

1. Run `build1934.py` against exact baselines with a fresh work directory.
   Require the host quality regressions, exact method/helper inventory,
   deterministic output and protection of camera startup/recovery and other apps.
2. Run `validate1934.py` against the pinned original ULike 5.6.2 (740) APKS for
   both standalone and bundle. Require real Morphe application, resource rebuild,
   arm64-v8a-only output, exact methods, DEX integrity, ABI and type verification.
3. Run `finalize1934.py` to bind QA, notes and source archive to those exact MPPs.
   Final release notes must describe the observed implementation, not plans.
4. Review and populate `manifest.json`: six artifact fingerprints, exact entry
   and method deltas, full source inventory and concrete positive host assertions.
   The unfilled `manifest.template.json` must fail publication validation.
5. Run `publish1934.py --local-only` with both local baselines. It is read-only.
6. Commit the reviewed source, evidence, manifest and publication paths to main,
   preserving other paths. Copy the archived workflow to
   `.github/workflows/ulike1934-quality-publish.yml` as the final trigger.
7. CI checks active feeds/policy, rebuilds identical tested MPP bytes, runs local
   validation and read-only preflight, then verifies uploaded release/raw bytes
   before advancing main/dev feeds atomically.
8. Inspect the receipt, both feeds, ULike update scope, unchanged other-app scope
   and public checksums. Inspect existing tags/receipts before any retry.

The workflow reuses local original-APKS application evidence by requiring exact
artifact hashes. It does not claim to run the original APKS in CI, Android/ART,
a physical device or a camera. It does not include APK/APKS or user images.
Every feed commit is a direct child of the corresponding leased branch HEAD.
Concurrent changes fail closed; the user's existing work and unrelated paths
are preserved.
