# Berry Browser compatibility v1.3.1 / Hiro bundle v1.0.272

This release repairs the existing Berry patch providers for the verified 3.83.93.1 input while retaining the 3.83.74 regression target. Version-independent Manager metadata in v1.3.0 did not remove the existing patch-time version checks, so it did not make the original three patches executable on 3.83.93.1.

## Scope

Only the original Berry provider namespaces may change:

- `app/hiro/berry/`
- `app/hiro/berryshare/`
- `app/hiro/berryzoom/`

The combined DEX and manifest change to carry the updated implementations/version. All unrelated entries, the MessageQueue JVM implementation, and all four existing Berry MPE extensions remain byte-identical. During patch application, the share provider adapts the injected helper's application references to the verified input profile; the unchanged packaged MPE does not imply identical injected helper definitions across app versions. The build performs DEX-level preservation checks as well. Each original patch must verify its target structure before editing it; broad Manager compatibility metadata does not imply universal future-version support.

The baseline is bundle `1.0.271`, SHA-256 `83ffc3416c99cef5d0464a147633f3d064a8b1856282715031176d2bb3cdbf7a`, fetched from its immutable `5b03455f033cfd8b4510d79d43753f57947d5482` distribution commit.

## Inputs and evidence

The local validation inputs are:

| Version | Original APK SHA-256 |
| --- | --- |
| 3.83.74 | `4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b` |
| 3.83.93.1 | `63d6f38c50c1cdca7a6c0bfed7abffa0183817aa33741b5bcb3bd287ef3342ac` |

The newer APK is a publicly distributed input, not a file extracted from the user's installed app. The saved older APK supplies regression coverage. APK/APKS binaries and user data are excluded from the public source package. Actual device behavior and performance have not been tested.

`publication/real-apk-evidence.json` binds those inputs and completed local test results to the exact candidate MPP hashes. Its schema is `berry-compat131-apk-evidence-v1`; its `inputs` list identifies both versions and APK hashes. All eight core application/rebuild runs passed: the two API32 inputs, each tested with the standalone and combined MPPs under Morphe Desktop 1.16.0 and 1.19.0-dev.19, with all four Berry patches selected. CI will validate this evidence and rebuild without those APK binaries.

Two additional 3.83.93.1 variant inputs received narrower application checks: API29 SHA-256 `fe0586a46ab1fa8b7386078b093f42585bc1383ef6c385fedfa73d07aa78a2b6` and API26 SHA-256 `0e604165f45511130265d0480f78cd42616400fa4ee990c9fc58df8bbbdbb5b0`. Both APKs verify against the same developer certificate as the API32 input, and all three have identical DEX bytes. Their manifest and resource table differ, so each additional variant was tested once with the combined MPP under Morphe Desktop 1.19.0-dev.19, selecting all four patches. Both application/rebuild runs passed, bringing the actual APK total to ten successful runs. Every output preserved its input's minimum SDK. These records use `additional_variant_inputs` and `additional_variant_runs`; they do not imply Android-device testing.

The official Morphe Manager 2.0.0-dev.16 host also selected all four requested Berry patches from the distributed combined MPP and verified unrestricted version metadata. This isolated host test exercised `PatchPipeline.selectedPatches` and `PatchKt.loadPatchesFromJar`; APK execution was covered separately by the ten Desktop patcher runs.

## Reproducible publication

1. Stage the exact tools in `src/tool-sources.json` and Temurin JDK 21.0.8+9.
2. Run the local APK validations and create candidate-bound evidence.
3. Build with `--without-real-apk`, using synthetic APK fixtures and the official Manager host, and obtain completed QA.
4. Run `publication/freeze.py --dist <dist>` only after the final source and candidate are verified.
5. Run `publication/package.py --dist <dist>` and `publication/publish.py --dist <dist> --base <baseline> --repo <repo> --local-only`.

The workflow will repeat two clean builds, require exact artifact equality and the frozen local identities, verify uploaded release assets, create immutable download URLs, and atomically advance both Manager feeds. Baseline/head checks protect concurrent changes and the ULike policy remains unchanged. The workflow is `.github/workflows/berry-compat131-publish.yml`, mirrored exactly in `publication/workflow.yml`.

Supplementary Actions verification artifacts are retained for 14 days. Published assets and the immutable distribution branch preserve the release. The Japanese release notes are `publication/README_Berry_Browser_v1.3.1.txt`.
