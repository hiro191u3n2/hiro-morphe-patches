# Processed-image gainmap core

This C++17 component computes an RGB gainmap from an **already processed** linear
HDR image and its matching linear SDR reference. It can reconstruct either
rendition and adapt reconstruction to display headroom. It has no Android,
graphics, codec, or third-party library dependency.

This is a completed, independently testable mathematical stage, **not a completed
camera, beauty engine, HEIF writer, or ISO 21496-1 binary metadata serializer**.
It does not turn an 8-bit SDR input into a genuine HDR capture. Existing app
capture/save routes do not call this component yet.

## Input and integration contract

- Inputs are nonnegative, finite, scene-consistent **linear RGB** FP32 or FP64.
  They must have matching primaries, SDR reference-white units, dimensions,
  capture ID, geometry ID, and processing revision. Unknown primaries, negative
  channels, mismatched frames, and invalid buffers are rejected.
- Supported primaries are sRGB, Display P3, and BT.2020. This code does not perform
  a color-space conversion. The source must already use those linear primaries.
  Negative values from a wider-gamut transform require a deliberate gamut
  mapping policy upstream; they must not be silently labelled valid here.
- `reference_white_nits` identifies the luminance corresponding to RGB 1.0.
  HDR and SDR must use the same value. P010 is a sample layout, not linear RGB.
  Its range, matrix, HLG/PQ transfer, and applicable OOTF/reference white must be
  handled before this stage. These conversions are not implemented here.
- The SDR reference must reflect the **same final beauty, geometry, color and
  exposure edit** as the HDR rendition. Recompute after the last image change.
  Do not append the capture's old map to a newly warped or relit face.
- Views, map views, and metadata carry matching frame descriptors. IDs are caller
  assertions, not proof of the pixels' origin or native processing history. A
  caller can still deliberately mislabel an unrelated buffer. Bind the map,
  metadata and base image together in the save job.
- Analysis and encoding are two passes over immutable buffers. Encoding catches
  out-of-range changes, but **not every mutation inside the old gain bounds**.
  Freeze or own the input buffers until both passes finish. Concurrent mutation
  is invalid and can also be a C++ data race.
- Each pass allocates at most one FP64 RGB row (`width * 3 * 8` bytes) plus small
  metadata. The caller owns source/destination storage. Map dimensions equal
  image dimensions; there is no hidden map downsampling or RGB averaging.
- Row sinks can stream directly into a later encoder/staging file. A thrown
  exception does not retract earlier rows. The caller must abort/delete the
  incomplete result and publish only after the complete pipeline succeeds.

## Defined math

For each channel, with SDR `S`, HDR `H`, and nonnegative offsets:

```text
L = log2(H + offset_hdr) - log2(S + offset_sdr)
n = (L - min_log2) / (max_log2 - min_log2)
G = pow(n, encoding_gamma)
decoded_L = min_log2 + (max_log2 - min_log2) * pow(G, 1 / encoding_gamma)
```

The difference of logs avoids overflowing a finite HDR/SDR ratio. Per-channel
bounds are the actual processed pair's minima/maxima. Constant-gain channels
have `G = 0` and reconstruct their constant `min_log2` without division by zero.
If both offset-adjusted channels are exactly zero, the otherwise undetermined
gain is canonically unity (`L = 0`); it still reconstructs black exactly. A
one-sided zero has no finite multiplicative gain and is rejected. The default
offsets are `1/64` in both renditions.

The metadata's gamma is the **ISO/Ultra HDR encoding gamma**. Android
`Gainmap.setGamma` describes the **decoding exponent** and must receive its
reciprocal (`Metadata::android_decoding_gamma()`). Log bounds must be converted
to linear ratios with `exp2` before using Android's ratio setters. Extreme
gammas which collapse an interior sample to 0 or 1 at FP64 precision are
rejected rather than silently destroying the gain information.

For SDR base, full reconstruction is:

```text
HDR = (SDR + offset_sdr) * exp2(decoded_L) - offset_hdr
```

For HDR base, full reconstruction is the inverse:

```text
SDR = (HDR + offset_hdr) * exp2(-decoded_L) - offset_sdr
```

Both directions retain the same **canonical HDR/SDR log ratio** in metadata.
Direction does not negate or rewrite the stored gain bounds.

For display headroom, `t` is the clamped logarithmic position between explicit
minimum and maximum display ratios. SDR base uses weight `t`; HDR base uses
`t - 1`. The base and alternate offsets swap for HDR base. As in Skia, zero
weight bypasses gain application and returns the base unchanged. Between
endpoints offsets are not interpolated. Unequal offsets can therefore imply
an endpoint discontinuity in the standard model. Adjacent very large headroom
ratios use endpoint comparisons and `log1p` arithmetic to avoid a zero log gap.

The core does not silently clamp negative reconstructed values. Quantization
and offset subtraction can produce slightly negative values near black;
downstream display/encoding must apply its explicit clipping/gamut policy.

## Precision and quantization

All gain/statistic/reconstruction arithmetic uses FP64. Source and normalized
map views accept FP32 or FP64; there is no implicit integer conversion. Optional
quantization accepts only 10 or 16 bits, rounds to nearest, and stores the codes
in `uint16_t` (`0..1023` or `0..65535`). The 10-bit layout is **not P010**: codes
are right aligned. Row stride is in scalar samples, not bytes. Encoder byte
order and pixel packing remain an explicit later step.

For gamma 1 and per-channel log range `R`, round-to-nearest map quantization
gives `abs(log_error) <= R / (2 * (2^bits - 1))`. Tests check the resulting
independently derived HDR error bound, as well as the pixel error. Quantization
is not lossless; this component does not claim bit-exact recovery after it or
after a future lossy HEVC encode. 10-bit containers also do not prove the camera
provided 10-bit information.

## Build and tests

```sh
make check
make sanitize
```

In this managed host, LeakSanitizer cannot inspect `/proc` task information.
`ASAN_OPTIONS=detect_leaks=0 make sanitize` passes AddressSanitizer and
UndefinedBehaviorSanitizer checks; leak detection itself is **not verified**.

Tests cover independently computed gain values, non-unity gamma, attenuation,
HDR-base inverse reconstruction, differing offsets, black, constant gain,
FP32 input, overflow-resistant ratios, padded rows, unknown/mismatched color
and geometry, wrong map identity, invalid metadata/buffers, 10/16-bit rounding,
quantization error bounds, and headroom/gamma numerical regressions.

The edit test changes geometry, local brightness and color, then regenerates
the map. It checks both rejection of the stale metadata and the actual large
reconstruction error if the stale map is deliberately relabelled, followed by
accurate reconstruction using the newly generated map. These are synthetic
host fixtures, not ULike style/phone/HEIF image-quality measurements.

## Remaining encoder work

A no-JPEG Android save job still needs real HDR capture, the independent beauty
pipeline, SDR reference generation, typed JNI/NDK integration, a 10-bit HEVC
encoder, a HEIF container writer with gainmap direction/color/metadata, and
device decoding/display validation. Upstream libultrahdr's HEIF implementation
currently writes SDR8 base/map planes; calling it unchanged does not satisfy an
all-stages-10-bit contract. This component performs no JPEG or HEVC compression
and writes no HEIF files.

## Primary references

- Android gainmap math and public metadata API:
  https://developer.android.com/reference/android/graphics/Gainmap
- Ultra HDR gainmap generation, encoding gamma and offset conventions:
  https://developer.android.com/media/platform/hdr-image-format
- Android/Skia direction, offset swap, zero-weight base bypass:
  https://github.com/google/skia/blob/main/src/shaders/SkGainmapShader.cpp
- Reference gainmap implementation (its optional near-black gain cap is not
  copied into this exact processed-pair reconstruction core):
  https://github.com/google/libultrahdr/blob/main/lib/src/gainmapmath.cpp

The published JPEG container specification is referenced for gainmap math only;
no JPEG container behavior is implemented or required here. ISO binary metadata
conformance is a separate, unfinished serialization/integration task.
