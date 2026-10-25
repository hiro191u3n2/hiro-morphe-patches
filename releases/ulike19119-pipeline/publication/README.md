# ULike 1.9.119 publication

The publisher accepts only the exact two MPPs and source/QA assets validated by `src/validate19101.py`. It verifies current 1.9.118 / 1.0.285 pins, publishes immutable assets, and updates main/dev feeds atomically with leases. Existing unrelated app bytes and feed scopes are preserved.

User authorization to publish automatically after the completed changes is recorded in the reviewed declaration. This does not bypass source, host, original-APKS, native identity, lineage, archive or live branch checks. Private APKs, toolchain files and photographs are excluded.

The matching `.github/workflows/ulike19119-pipeline-publish.yml` reproduces the packages, verifies the exact SHA256/size and complete source/publication pins of the already executed immutable v1.9.118 host report, freshly executes both v1.9.119 host groups, binds the previously executed original-APKS evidence to identical output hashes, validates every release asset, then publishes. The optional new noise natives require independent runtime exact-output and speed certification; target-device speed is not claimed from the host checks.

The 89 historical baseline groups remain labelled `immutable_v118_baseline_regression`; reusing their pinned report does not claim they ran again in the current CI job. Current candidate and changed-runtime tests are never replaced by this historical evidence. Public-safe synthetic host test logs and result reports are retained as a separate diagnostic Actions artifact on success or failure.
