# ULike 1.9.45 exact ARM64 residual kernels

The v1945 library keeps the existing `libulike_speed1935.so` filename and all v1935/v1944 JNI exports and ABI versions. `NativeSpeed1944` still performs processed-domain residual noise and periodic texture aggregation. Existing v1935 YUV and resampling algorithms are unchanged.

Build the Android library with official NDK r27c (27.2.12479018):

```bash
python build_native1944.py --ndk /path/to/android-ndk-r27c --output /path/to/native-output
```

API 26, ARM64 only; no runtime library dependency, no fast-math or floating contraction, all ELF LOAD segments aligned to 16 KB. The build report pins byte length, SHA-256, source hashes, exported JNI symbols and compiler version. Resource installer and DEX loader must pin the **new** byte length and hash while retaining the existing two-row inventory and `ulike1935/runtime/libulike_speed1935.so` resource path.

One JNI call aggregates a bounded processed strip. The Java caller computes metadata from the original local noise estimate and face/detail policy, retaining all floating-point budget coordination and final rounding in Java. Native code caches exact luma, chroma differences and opacity in an exclusively leased row ring; it preserves sparse/contiguous neighbor coordinates, tap order, spatial and range weights, half-window edge means, periodic texture direction/phase gates, integer divisions and saturation bounds. Three integers per pixel carry fine/coarse averaged RGB and packed range/edge/texture summaries. Source pixels are immutable and summaries never write into the image output.

The production caller limits each JNI invocation to 32 rows. Workspace is exclusively leased from the bounded SpeedWorkers pool and released after each native tile. No pixel cache remains attached to a thread or spans photographs. All arrays and integer bounds are checked before summaries are written. Library or symbol unavailability and allocation failure retain the exact Java fallback.

P10 adds explicit four-center integer NEON operations to row packing and residual aggregation. Luma/chroma differences, opacity/threshold masks, min/max, half-window sums and weighted RGB accumulations process four independent pixels together. Range-table lookup, chroma-weight division, final averaging and periodic texture decisions remain exact scalar integer operations. No approximate reciprocal, floating conversion, reassociation, coefficient change or sample reduction is used. Inactive lanes cannot index the range table with unchecked metadata.

P7 resolves vertical tap row addresses once per row. Interior columns use contiguous addresses, while edge columns retain exact repeated-edge clamping. Narrow images and SIMD tails use the scalar path. A once-per-tile canonical-table check preserves the previous C/JNI behavior for arbitrary in-bounds x tables by falling back to table-based scalar reads.

For the existing opaque resampler, v1944 groups a target row's vertical taps into a single JNI invocation when all referenced source rows fit simultaneously in the existing LRU cache. It calls the retained v1935 exact vertical float kernel in the original tap order. The horizontal coefficients, cached row values, float extrema and final Java rounding remain unchanged. When the required support exceeds cache capacity, the previous per-tap path remains active. The test oracle includes the previous `FastPixels1933` implementation and compares complete resized RGB images as well as raw float bits against Java and the previous native JNI path.

Host JNI differential validation:

```bash
python ../host_native1944.py --jdk /path/to/jdk --output /path/to/host-evidence
```

`Native1944Test.java` is a direct-source independent transcription of the previous integer aggregation, without row-cache/native helpers. It covers radius 1–4, random RGB, smooth grain, repeated texture, alpha rejection, valid processed halo bounds, width-one images, 4080-pixel strips, repeated ring reuse and parallel callers. This validates algorithm equality and actual JNI glue on the host. It does not measure a physical Galaxy's speed or verify end-to-end Android capture.

P10/P7 validation, including actual execution of NDK-compiled AArch64 NEON instructions:

```bash
python ../host_native1945.py --jdk /path/to/jdk --ndk /path/to/android-ndk-r27c \
  --cross-gcc /path/to/aarch64-linux-gnu-gcc --qemu /path/to/qemu-aarch64 \
  --output /path/to/native1945-evidence
```

For a locally extracted cross-toolchain, pass `--cross-library-path` with its host shared-library directory. The same NDK r27c compiler and per-kernel flags used for the Android library compile the ARM64 kernel object; a Linux harness links that object for qemu-user execution. An independent direct-source C oracle covers every width 1–97, all four radii, 4080-pixel strips, inactive metadata with arbitrary nonactive bits, cropped support bounds, alpha rejection, noncanonical x tables and random/constant/zero range weights. Guard pages immediately follow all input, output and scratch arrays. Host ASan/UBSan and the unchanged Java JNI oracle run as well. Emulated instruction validation establishes exactness, not physical Galaxy save latency.
