# ULike v1.9.122 source and QA

This package contains the exact Java/native reference source closure and local evidence for ULike v1.9.122. The matching combined release is Hiro Morphe Patches v1.0.328. Physical Android execution, performance and visual quality are not claimed.

Use Temurin JDK 21.0.8+9 (21.0.8+9-LTS), the pinned Morphe 1.16.0 JAR, Android platform-35_r02 android.jar and build-tools_r35_linux d8.jar. Their SHA-256 pins and the observed JDK identity are in evidence/build19122.json. Toolchain binaries and original APKs are not included.

Build from this directory with fresh output/work directories:

```sh
python3 src/build19122.py --baseline baseline121/ULike_HQ_Texture_Online_v1.9.121.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families responsive-families19122.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

The ten host suites are background, cached restore, idle cleanup, diagnostic refresh, GPU completion wait, GPU submission, comparison policy, actual save integration/preview, quiet runtime and responsiveness. Exact commands and report paths are in the same GitHub release source tree at publication/ci-suites19122.json. Run every suite against the packaged src directory and the helper-classes from this exact build. Host assertions, mutation controls and controlled failure scenarios are reported separately in QA_ULike_v1.9.122.json; APK reflection assertions are not included in host totals.

For the private original-APKS application check, use src/apply_original19122.py with its --help options and the exact ULike 5.6.2 (740) APKS SHA-256 recorded in evidence/application19122.json. The APKS, merged/unsigned APK and private logs must remain outside this source tree. This rebuild does not sign, install or execute Android code on a device. The preview reflection proof is bound to the actual standalone-applied APK; it is not a device rendering test.

Normal mode preserves the image-processing arithmetic and output settings, with lower CPU/GPU concurrency, post-save idle cache cleanup and bounded observations. The explicitly selected one-shot diagnostic bypass changes image processing and is not a normal-quality mode. Cached GPU certification does not authorize new unverified candidates. Automatic additional qualification stays disabled.

The v1.9.121 standalone baseline is pinned under baseline121. Earlier metadata and evidence remain under inherited121 and inherited120 as historical provenance; only the current root QA and evidence directory describe this release. Build-stage pending host/application fields are superseded by the completed evidence listed in root QA.

manifest.json lists every ZIP entry except itself with bytes and SHA-256. The ZIP sorts entries by pathlib.Path, fixes timestamps to 2026-11-02 00:00:00, uses mode 100644 and DEFLATE. Publication CI reconstructs the ZIP and MPPs, reruns all ten host suites, verifies original-APKS proofs and preserves unrelated combined-bundle applications before any feed update.
