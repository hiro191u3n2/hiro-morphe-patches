# ULike v1.9.124 source and QA

This source package rebuilds ULike v1.9.124 and records its original-APKS application evidence. Matching combined release: Hiro Morphe Patches v1.0.336. Android installation, camera execution, responsiveness and visual quality remain unverified.

The compatibility fix requests resource extraction before three existing-file reads/checks. Two sites belong to original asset/native edits; one preserves collision checking before native-runtime installation. The manifest parent-path lookup remains unchanged. Original hash/instruction/collision guards and all asset payloads are preserved. Stock methods, .123 CPU boost suppression, normal image arithmetic, output dimensions, preview/save ownership and existing load controls remain unchanged.

Build uses Temurin JDK21.0.8+9-LTS and the pinned legacy compiler toolchain listed in evidence/build19124.json. Actual application and the new resource suite use Morphe Desktop2.0.0-dev.4 with exact embedded patcher2.2.0-dev.2; modern tool SHA-256 ed8184451a88b373ab38e575884523a709706195a7232e971fcf8cd61fc5a22c. Both standalone and combined manifests target patcher2.2.0-dev.2 and are tested with on-request decoding. Toolchain binaries and original APKs are excluded.

```sh
python3 src/build19124.py --baseline baseline123/ULike_HQ_Texture_Online_v1.9.123.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families resource-families19124.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

Run all twelve host suites using publication/ci-suites19124.json in the matching release source tree. Assertion totals are checks, not independent cases; mutation controls and expected-failure scenarios are distinguished. Static stock-boost inventory checks and APK reflection checks are separate from host functional assertion totals.

Use src/apply_original19124.py for the private original ULike5.6.2(740) APKS identified in evidence/application19124.json, with modern Morphe JAR named morphe.jar in --tools and the old pinned parser JAR in --validator-tools. The old parser only evaluates existing canonical DEX contracts after application; it never performs the actual new patch operation. The combined application wrapper is publication/apply_combined19124.py. Both require actual on-request decode logs, new CLI success/stepResults, FULL DEX rebuild, all three modified asset ZIP outputs, all 217 original assets, all original/native payloads and exact helper linkage. Private original/merged/patched APKs and raw logs must stay outside this package.

The old desktop1.16.0 embedded patcher1.14.0 and did not cover the modern combined bundle's on-request behavior. Even modern-engine standalone123 testing used legacy all-resource decoding because its manifest targeted1.8.0. evidence/reproduction19124.json records the actual combined333 failure with asset81001 present in the input. This compatibility gap is now explicitly covered; guards were not removed to make the test pass.

The v1.9.123 standalone baseline is pinned under baseline123. Earlier metadata and evidence are historical under inherited123 and older inherited directories. Only root QA and current evidence describe this release. manifest.json binds all packaged files except itself. ZIP ordering, timestamp2026-11-03, mode100644 and DEFLATE are deterministic. Publication CI rebuilds byte-identical MPPs/source ZIP, reruns all twelve hosts, validates the private application proofs, and preserves other bundled applications before updating feeds.
