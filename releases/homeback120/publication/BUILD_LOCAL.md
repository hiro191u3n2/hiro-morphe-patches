# Local build and evidence commands for v1.1.10

The active base is Hiro Morphe Patches v1.0.247, containing OneBack v1.1.9 and ULike v1.9.105. Bundle v1.0.248 is reserved by the previously committed ULike v1.9.106 work; this release uses v1.0.249. A source-only ULike v1.9.106 commit is not a published bundle and must not replace the active binary baseline.

The exact baseline URL and SHA-256 are in `manifest-template.json`. The downloaded base must hash to `82e7bdca6288963465fb6cbc17d85ccdf827aa8ddbecb62faa48e65c3d926703` (18,170,134 bytes). Its OneBack v1.1.9 runtime must hash to `1f188bfa1d11e66d367db94a5cb18ec6da140e34a63f7a389417160c88795a54`.

## Toolchain

Local tools live in `/workspace/scratch/075f7970e1d9/build-inputs/tools`. Use Temurin JDK 21.0.8+9 from `/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9`. CI selects the same release. The Morphe, Android SDK and D8 hashes remain fixed in `src/build.py`. The workflow downloads public upstream archives and verifies archive and extracted file hashes; it does not depend on expiring Actions artifacts.

## Required evidence before final build

Re-run the supplied-APK ABI audit against the newly compiled runtime for all three exact, already-patched APKs; regenerate `provided_apk_audit_summary.json`. Re-run the baseline ABI audit with the runtime extracted from active bundle247, recording baseline OneBack v1.1.9 and replacement v1.1.10. Existing evidence schema names and regression classes are retained so their established meaning remains stable. Static layout fixture bytes are unchanged. Re-run the native Reels boundary audit on both supplied Instagram APKs, update its source/log hashes, and include the new selected-Reels dispatch regression evidence. Do not rewrite old PASS results to imply a new run.

None of these checks establishes Android physical-device behavior. Do not modify or redistribute the supplied APKs as part of this release.

## Build, freeze and validate

Run after the runtime, patch loader, tests, audit sources and evidence are final. Use fresh work and output directories. The source tree must remain unchanged for the duration of the builds.

```bash
set -euo pipefail
cd /workspace/scratch/075f7970e1d9/hiro-morphe-patches
export PATH="/workspace/scratch/075f7970e1d9/build-inputs/jdk-21.0.8+9/bin:$PATH"
task_tools=/workspace/scratch/075f7970e1d9/build-inputs/tools
task_base=/workspace/scratch/075f7970e1d9/build-inputs/baseline247.mpp
task_release=releases/homeback120
task_work=/workspace/scratch/075f7970e1d9/build120-final
task_dist=/workspace/scratch/075f7970e1d9/dist120-final
python3 "$task_release/src/build.py" --base "$task_base" --tools "$task_tools" --legacy releases/oneback163/src --work "$task_work" --dist "$task_dist"
python3 "$task_release/publication/freeze_local.py" --dist "$task_dist"
python3 "$task_release/publication/package.py" --dist "$task_dist" --legacy releases/oneback163/src
python3 "$task_release/publication/publish.py" --dist "$task_dist" --repo . --base "$task_base" --local-only
```

Finish `README.md` and `publication/RELEASE_NOTES.txt` with the actual findings and validation limits before final packaging. The local freeze records real MPP/QA/audit identities; stale manifests are not sufficient.

CI makes two fresh builds, verifies both MPP and QA against the frozen local identities, packages the exact sources, and executes the full local and remote preflight gates. Publication requires the current GitHub Actions run and source commit, unchanged main/dev baseline feeds, an unused release tag, exact asset identities, and atomic branch updates. Do not bypass the two-build or active-baseline gates. Only the Instagram update scope is added to the Manager changelog.
