# Berry Browser v1.3.0 / Hiro bundle v1.0.271

This release adds an Android 17 MessageQueue opt-in patch and removes the Berry app-version restrictions from Manager compatibility metadata. Existing patch-time version and bytecode checks remain in the three earlier Berry patches.

## Behavior

The new patch keeps `targetSdkVersion` at least 37, declares the local-network permission and provides its runtime request and MessageQueue diagnostics. Android 17 implements the new queue; this release does not backport it to older Android versions. The saved Berry 3.83.74 sample already targets API 37, so its target level alone provides no newly demonstrated speed gain. Device behavior and performance have not been measured.

The earlier Berry extensions remain byte-identical. The combined package preserves all unrelated MPP entries and DEX definitions. Android DEX metadata and JVM patch implementations are both shipped, avoiding a displayed-but-unexecuted patch in current Manager versions.

## Reproducible validation

The active baseline is `Hiro_Morphe_Patches_v1.0.270.mpp`, SHA-256 `05064b69f2965146de6461d7cbcf25318e61f17a2f099b25d6c15c2a205c821e`.

1. Stage pinned tools from `src/tool-sources.json` and Temurin JDK 21.0.8+9.
2. Run the local build with `--apk` to validate the saved private Berry APK. Record the candidate-bound evidence in `publication/real-apk-evidence.json`; do not commit or publish the input APK/APKS.
3. Run reproducible local builds with `--without-real-apk`, exercising public synthetic fixtures and the official Manager host while loading the recorded local evidence.
4. Freeze the completed results with `publication/freeze.py --dist <dist>`.
5. Package with `publication/package.py --dist <dist>` and check `publication/publish.py --dist <dist> --base <baseline> --repo <repo> --local-only`.

The workflow repeats two clean public-input builds, checks exact artifact identities and frozen sources, then publishes the reviewed assets and updates both Manager feeds atomically. Neither local static APK validation nor CI is represented as Android-device testing.

## Publication

The workflow is `.github/workflows/berry-messagequeue130-publish.yml`, with an identical copy in `publication/workflow.yml`. Publication shares the repository's existing release concurrency group, verifies current baseline and heads, preserves the ULike policy, verifies uploaded release bytes and immutable download URLs, and advances `main`/`dev` together. Build logs remain as supplementary Actions artifacts for 14 days; release assets and the immutable distribution branch preserve deliverables.

The release notes are `publication/README_Berry_Browser_v1.3.0.txt`. Only patch source, reproducible evidence, QA and generated MPPs are public.
