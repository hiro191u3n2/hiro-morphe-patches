# Neural output to makeup/LUT staging adapter

`PostNeuralStyleSink` implements the actual
`BeautyImageEngine.TransactionalSink` interface. It consumes the neural engine's
borrowed packed FP32 RGB rows, converts a bounded tile to FP64, calls the verified
`StyleLutPipeline.runPostNeural`, and sends FP32 output rows to a private staging
sink. No integer or 8-bit photo conversion is used, and full raster dimensions
are unchanged. This adapter is source integration, not an installed app hook.

Construction requires the exact owned still-frame object, capture ID, sensor
timestamp, oriented full dimensions, explicit SDR domain and the explicit
replacement graph policy. `begin` must receive the same frame object by identity
(`==`), not merely an equal string or timestamp. The `BindingProvider` must return
the same object plus sampled textures, geometry coverage, segmentation and
resolved settings for each requested tile. Individual pass/LUT tile records must
also match their capture ID, timestamp and coordinates. These checks reject
declared mismatches; the caller is responsible for obtaining true same-frame
geometry rather than assigning matching labels to preview results.

The graph policy means declared outer z-orders plus caller-resolved source
textures and equal-z renderer order. It is an explicit replacement policy, not
evidence of the original SDK's complete active graph. Texture source routing is
particularly relevant for Purity blusher and 3D makeup. This adapter does not
generate the required inputs, infer slider-to-uniform values, or synthesize a
skin mask.
For Purity blusher, the provider must fold interpolated per-vertex `varOpacity`
into its per-pixel coverage (`geometryCoverage * varOpacity`), with scalar
opacity 1 or a separate global factor. Passing only a uniform opacity would
omit that vertex-dependent behavior; this adapter does not rasterize it.

Input row groups must be contiguous and inside the frame. They are split into
smaller full-width groups to meet both the pixel and primitive-array allocation
budgets. The final borrowed neural buffer may be larger than the final row count;
only its declared prefix is read. NaN or stale data outside that prefix is ignored.
All consumed samples must be finite encoded SDR unit values. A one-row minimum
budget is checked before a downstream staging transaction begins.

Commit requires every row. Provider, style, downstream write, begin and commit
failures trigger rollback; subsequent abort calls are idempotent. Cleanup
failures are suppressed onto the original failure. A successful committed image
is never removed by a later abort. The downstream sink must itself honor the
documented staging/atomic-commit contract; this adapter cannot make a sink that
publishes early transactional.

The memory counter is the **sum of primitive pixel arrays allocated per tile**
by the adapter and invoked style stages, not a heap-peak claim. It counts four
FP64 RGB arrays for Natural or thirteen for Purity, plus one FP32 output array.
Caller source, bindings, LUT storage and staging, neural/native allocations,
object headers and garbage-collector retention are excluded. Standard settings
allow 262,144 tile pixels and 32 MiB of those counted allocations; actual row
groups are reduced to meet both limits. No 24.5 MP sensor-input or 512 MiB phone
heap guarantee follows from these host-side bounds.

## Verify

```sh
JAVA_HOME=/path/jdk21 ANDROID_JAR=/path/sdk36/android.jar \
ORT_CLASSES_JAR=/path/onnxruntime-android-1.30.0-classes.jar \
R8_JAR=/path/r8-8.3.37.jar bash test.sh
```

This compiles against SDK 36 with the real neural/model-runtime interfaces,
executes host transaction/shape/precision tests, and compiles production DEX.
The author tests cover identity preservation at zero effect, analytic nonzero
makeup, row subdivision/remainders, unused buffer tails, frame/tile mismatch,
partial commit, provider/write/begin/commit failures and cleanup suppression.

`test_real_models.sh` additionally executes both pinned actual neural models and
the supplied neural masks through `BeautyImageEngine` and this adapter. With
explicitly synthetic sampled makeup/geometry, it compares every 512×384 FP32 RGB
output sample against an analytic edit of the direct neural result, and checks
that a late binding failure rolls back exactly once across the outer engine and
the adapter. See `QA_REAL_MODEL_ADAPTER.json`. Required arguments are the two
original model files, original `libbytenn.so`/`libeffect.so` and the two supplied
style ZIPs; set `JAVA_HOME`, `ANDROID_JAR` and `ORT_HOST_JAR` to the existing host
toolchain. Model and library pins are checked by the real model loader.

These tests do not run a phone camera or GPU, create missing real face geometry
or segmentation, establish full visual style parity, or make SDR-authored
effects HDR-safe. Actual makeup asset shader tests are recorded separately by
the parent module; this integrated fixture deliberately distinguishes those
tests from its synthetic geometry. HDR/gainmap processing and the installed
patch connection remain separate work.
