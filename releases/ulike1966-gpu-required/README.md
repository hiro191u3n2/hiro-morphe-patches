# ULike v1.9.66 / bundle v1.0.199 — GPU required

Build candidate on the exact published ULike v1.9.65 / bundle v1.0.198. Enabled noise, protection-mask and additional correction image calculations must use GPU. CPU orchestration, copies, logging and file/codec management remain. Existing ULike beauty SDK and face-detection internals are not proved GPU-only.

GPU precision or processing failure preserves the original source, permits bounded GPU recovery, then fails processing/save. The candidate does not return to CPU image arithmetic or publish untreated pixels as success. Hardware FP64 or the exact GPU integer binary64 implementation must pass an executed precision probe; unsupported precision must not be described as successful GPU migration.

Sources and tests are under `src`; publication operations and atomic main/dev feed guards are under `publication`. The workflow only publishes after all fresh compilation, source-pin, native-linkage, test and package checks succeed. A functional original-GLES path is required before this candidate can be published.

No physical Android capture, original-APKS application, saved-image quality or device speed claim is made. Software host GPU execution and desktop GLSL adaptations do not establish Galaxy support.

The GPU binary64 implementation includes Berkeley SoftFloat derived BSD 3-clause code. The complete notice is shipped in each MPP as `ulike1965/THIRD_PARTY_LICENSES.txt`, and in the source archive as `THIRD_PARTY_LICENSES.txt`.
