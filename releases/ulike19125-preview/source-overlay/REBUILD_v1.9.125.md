# ULike v1.9.125 source and QA

This package rebuilds ULike v1.9.125 with exact public v1.9.124 as baseline. Matching bundle: 1.0.338 over unchanged active bundle 1.0.337. Android execution, displayed responsiveness and visual quality remain unmeasured.

The existing guarded HD preview release is enabled after save input protection. Capture generation, image ownership, activity/lifecycle, camera route and source bitmap guards remain. Preview release requests and later input notices are distinct measurements; neither proves displayed frame completion. Numerical image-processing helpers, output resolution/formats, stock methods, CPU/GPU scheduling, disabled automatic qualification, fourteen native payloads and the .124 resource extraction fix remain inherited.

Build with Temurin21.0.8+9-LTS and pinned compiler tools:

```sh
python3 src/build19125.py --baseline baseline124/ULike_HQ_Texture_Online_v1.9.124.mpp --tools "$ULIKE_TOOLS" --jdk "$ULIKE_JDK" --families preview-families19125.json --work "$ULIKE_BUILD_WORK" --output "$ULIKE_OUTPUT"
```

Run the publication/ci-suites19125.json plan. Assertion totals count checks; mutation and controlled-failure counts are kept distinct. The resource suite runs the actual .125 candidate against real modern ResourcePatchContext. Its single display description is normalized to the prior display metadata solely for the historical complete-code inverse; the three resource reads and every guard/instruction remain verified, including ten structural mutations and six controlled failures.

Use src/apply_original19125.py and publication/apply_combined19125.py with modern Morphe Desktop2.0.0-dev.4 / embedded patcher2.2.0-dev.2. --validator-tools uses the pinned historical serializer only for canonical contract checks. Actual patching uses current on-request decoding and FULL DEX rebuild. All 217 original assets, three replaced asset ZIPs, native payloads, helper type/member linkage and the reflection chain must pass. Original and patched APKs, raw logs and toolchains are excluded.

Root QA/current evidence describe .125. evidence/reproduction19124.json is the historical .124 patcher/resource regression evidence, not a new .125 user-device reproduction. manifest.json binds every packaged file. Source ZIP ordering, timestamp2026-11-03, mode100644 and DEFLATE are deterministic. Publication CI reconstructs the full source tree from hash-pinned public .124 bytes plus a fully pinned delta, reproduces both MPPs/source ZIP and reruns every declared host suite before atomic Manager feed updates.
