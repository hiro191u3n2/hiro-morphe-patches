# ULike v1.9.123 source and QA

This package contains the exact Java/native reference source closure and local evidence for ULike v1.9.123. The matching combined release is Hiro Morphe Patches v1.0.333. Physical Android execution, performance and visual quality are not claimed.

Use Temurin JDK 21.0.8+9 (21.0.8+9-LTS), the pinned Morphe 1.16.0 JAR, Android platform-35_r02 android.jar and build-tools_r35_linux d8.jar. Their SHA-256 pins and the observed JDK identity are in evidence/build19123.json. Toolchain binaries and original APKs are not included.

Build from this directory with fresh output/work directories:

```sh
python3 src/build19123.py --baseline baseline122/ULike_HQ_Texture_Online_v1.9.122.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families stockboost-families19123.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

The eleven host suites are background, cached restore, idle cleanup, diagnostic refresh, GPU completion wait, GPU submission, comparison policy, actual save integration/preview, quiet runtime, responsiveness, and stock-boost suppression/inverse verification. Exact commands and report paths are in the same GitHub release source tree at publication/ci-suites19123.json. Run every suite against the packaged src directory and the helper-classes from this exact build. Host assertions, mutation controls and controlled failure scenarios are reported separately in QA_ULike_v1.9.123.json; APK reflection assertions are not included in host totals.

For the private original-APKS application check, use src/apply_original19123.py with its --help options and the exact ULike 5.6.2 (740) APKS SHA-256 recorded in evidence/application19123.json. The APKS, merged/unsigned APK and private logs must remain outside this source tree. This rebuild does not sign, install or execute Android code on a device. The preview reflection proof is bound to the actual standalone-applied APK; it is not a device rendering test.

This revision suppresses only the optional stock still-photo CPU boost callsite. Normal image-processing arithmetic, output settings, existing CPU/GPU concurrency limits, post-save idle cache cleanup and bounded observations are preserved. The explicitly selected one-shot diagnostic bypass changes image processing and is not a normal-quality mode. Cached GPU certification does not authorize new unverified candidates. Automatic additional qualification stays disabled.

The v1.9.122 standalone baseline is pinned under baseline122. Earlier metadata and evidence remain under inherited122, inherited121 and inherited120 as historical provenance; only the current root QA and evidence directory describe this release. Build-stage pending host/application fields are superseded by the completed evidence listed in root QA.

manifest.json lists every ZIP entry except itself with bytes and SHA-256. The ZIP sorts entries by pathlib.Path, fixes timestamps to 2026-11-03 00:00:00, uses mode 100644 and DEFLATE. Publication CI reconstructs the ZIP and MPPs, reruns all eleven host suites, verifies original-APKS proofs and preserves unrelated combined-bundle applications before any feed update.
