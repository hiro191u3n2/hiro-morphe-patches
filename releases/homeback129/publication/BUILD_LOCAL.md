# HomeBack129 local build and publication preparation

Release: OneBack v1.1.19 / aggregate v1.0.278. Baseline: immutable published aggregate v1.0.277, SHA-256 `797e102352dfb46fc38f0ffb4a8d65066a843079d5089be8f390306ac3c4281a`.

Use the tool hashes and public download locations in `src/build.py` and `publication/workflow.yml`, with Temurin JDK 21.0.8+9. Use fresh build/output directories.

```bash
python3 releases/homeback129/src/build.py --base /absolute/baseline277.mpp --tools /absolute/pinned-tools --legacy releases/oneback163/src --work /absolute/new-build-dir --dist /absolute/new-dist-dir
```

The build compiles against the production Android SDK, executes all checked-in runtime host suites, audits DEX structure and unrelated loader/resource preservation, reproduces the published prior-patch exception with obfuscated-name collision fixtures, verifies package-scoped application and malformed-Instagram rejection, checks official loader metadata, and performs 30 synthetic full-patcher application/rebuild cases. Non-Instagram fixtures include colliding class names. The runtime must be byte-identical to the published baseline. Existing runtime-upgrade references remain unchanged; no new upgrade reference is required.

This release does not have the reported TikTok 47.4.3 original APK. It does not claim real-APK, 123-patch coapplication, Android installation, screen navigation or recent-task verification. The evidence record binds executed synthetic checks and final MPP/QA identities only.

```bash
python3 releases/homeback129/publication/create_local_evidence.py --dist /absolute/new-dist-dir
python3 releases/homeback129/publication/freeze_local.py --dist /absolute/new-dist-dir
python3 releases/homeback129/publication/package.py --dist /absolute/new-dist-dir --legacy releases/oneback163/src
python3 releases/homeback129/publication/publish.py --dist /absolute/new-dist-dir --repo . --base /absolute/baseline277.mpp --local-only
```

`create_local_evidence.py` writes `local-scope-validation.json`; `freeze_local.py` binds its hash, local MPP bytes, QA and host results into the manifest. Do not reuse earlier releases' proof files or expected hashes.

Review the current main/dev heads and unchanged active277 feed before submitting the source commit. That commit must directly descend from `reviewed_main_head`; dev must still match `reviewed_dev_head`. Copy `publication/workflow.yml` to `.github/workflows/homeback129-publish.yml` unchanged before submission. The workflow executes two clean builds, requires local/CI MPP and QA byte equality, packages the authored source and scope evidence, performs preflight, and then publishes. Immutable download verification and atomic main/dev updates with explicit leases preserve the existing feed history and ULike active policy. Changed heads or baseline require reviewing the new state, not weakening checks.
