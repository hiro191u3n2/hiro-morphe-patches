# ULike v1.9.56 / Hiro Morphe Patches v1.0.189

H28–H33 apply exact optimizations to the immutable published ULike v1.9.55 / bundle v1.0.188 baseline. NR1–NR4 single-image processing, image-quality settings, capture-fusion rejection, codec settings and unrelated applications are retained. Five existing native libraries are rebuilt, four are byte-preserved, and the native installer retains nine rows. No APK, APKS, photograph or signing key is included.

The implementation covers vectorized CPU denoising and linked horizontal/vertical passes; exact no-op residual calculation elision; certified GPU dispatch persistence bound to runtime and route fingerprints; bounded staging allocation reuse; and overlap between post-encode publication and the next photo's encoding. Qualification, cancellation, ownership, memory limits, fallback and FIFO publication are verified by focused host suites. Physical Galaxy timing and end-to-end device behavior remain unverified. The original APKS has not been used for this release's patch-application test.

## Reproducible build and evidence

`src/build1956.py` requires pinned ULike/bundle inputs, Morphe/Android/D8 tools, Temurin 21.0.8+9-LTS and Android NDK r27c (27.2.12479018). `src/validate1956.py` checks the built archive, exact DEX/resource scope and fresh host/source evidence. `src/finalize1956.py` creates six release assets, source checksums and the generated publication manifest. The source archive includes the exact implementation and publication workflow used by CI.

CI installs `qemu-user`, `gcc-aarch64-linux-gnu`, `libegl1-mesa-dev` and `libgles2-mesa-dev`, and sets `ULIKE_REQUIRE_ARM_NEON=1`. The host report must contain positive executed assertions from the NR1–NR4 preservation, H28/H29, H30, H31, H32, H33 and scratch-memory-budget suites. Emulated AArch64/NEON execution does not establish physical-device speed.

## Publication

`publication/publish1956.py` uses the independently generated `--expected` manifest to verify all six asset hashes, source hashes, exact changed/added resource sets, QA values and native payloads. `--local-only` requires both `--baseline` and `--standalone-baseline` and performs no remote writes. `--local-only` may validate an honest local draft when AArch64 execution tools are unavailable. Every remote preflight/publication additionally requires executed H28/H29 and H30 AArch64 NEON proofs; the lane model alone is insufficient. `--preflight-only` then checks both live Manager feeds and the active ULike policy without writing.

The live publication creates and verifies draft assets before publication, verifies public bytes, records immutable MPP download URLs, then atomically advances the `main` and `dev` Manager feeds using checked HEAD leases and direct-descendant commits. A private index preserves the checkout and unrelated files. The changelog adds only ULike scope and retains prior history. It records a publication receipt and re-downloads the final assets and feed targets. An exact already-completed retry verifies its receipt and assets without remote writes; incomplete or differing attempts stop for inspection.

The archived `publication/ulike1956-h28-h33-publish.yml` must exactly match `.github/workflows/ulike1956-h28-h33-publish.yml`. Only those explicitly selected source and publication paths should be committed when preparing this release. Actual publication is performed separately after the build and checks pass.

## User installation

Morphe Managerでパッチソースを更新し、未改造のULike 5.6.2（740）にパッチを再適用してください。ソース更新だけでは既存のインストール済みアプリは更新されません。単体と総合版はどちらか一方を使用してください。
