# Verified style color components

This C++17 source implements two pointwise color operations from the supplied
Natural blush and Purity2 shader assets. It is a partial component of a proposed
engine, not an Android patch, a complete style, or an HDR beauty pipeline.

| API | Material shader | Operation |
| --- | --- | --- |
| `purity_final_lut` | Purity2 `materials/016/AmazingFeature9/xshader/pass0.frag:11–33` | Sample the two adjacent blue slices of an 8×8 LUT atlas, interpolate by fractional blue, then blend with the input by `uniAlpha`. |
| `skin_background_lut` | Natural `materials/016/AmazingFeature2/xshader/skinseg.frag:14–33`; identical Purity2 `AmazingFeature8` | Sample only the floor blue slice from both LUTs, blend background/skin by an externally supplied skin mask, then blend with the input by `uniAlpha`. |

Both preserve input alpha. The LUT addressing constants are copied literally:
`tile / 8 + 0.5 / 512 + (1 / 8 - 1 / 512) * input.rg`.
The supplied Purity final LUT is **1024×1024**, but its shader still uses 512 in
these expressions. The component preserves that behavior. It does not silently
replace the skin shader's floor-only lookup with blue-slice interpolation.

## Required contracts

The caller supplies matching authored/input RGB primaries and transfer, full
range, and a sampler policy. Only encoded SDR sRGB or gamma-2.2 domains are
accepted; linear, HLG, PQ, limited-range, mismatched, nonfinite, and out-of-range
values are rejected. The asset files do not establish their authored standard
RGB encoding. Selecting an enum is an explicit caller assertion, not a verified
property of the original engine. No gamut conversion or transfer conversion is
performed here.

LUT texels must be normalized RGBA in [0,1]. Texture row zero is the row sampled
near `v=0`. The caller controls decode orientation, ICC/gamma handling, and the
original engine's sampler choice. The tests use decoded asset sample values,
source row order, and linear clamp-to-edge. The Purity final LUT named
`filter.png` is actually JPEG/JFIF; its existing authored compression cannot be
undone. This does not introduce JPEG compression of the photograph. That
contract is not proof of the
original runtime's decode/upload behavior.

`skin_background_lut` requires the matching pixel's mask alpha after the source
shader's vertical mask-coordinate flip. It does not generate a skin mask, face
landmarks, neural style images, tracking, or a complete rendering graph. It does
not infer `uniAlpha` from exported API history.

The implementation uses double-precision arithmetic. It matches the shader's
algebra on the declared domain; original GPU `lowp` rounding is not bit-matched.
Although the shader clamps its input, this API rejects HDR values instead of
discarding highlights. Applying an SDR style to HDR requires a separately
specified color/appearance transform and validation, not removal of this guard.

## Reproduce validation

Requirements: C++17 compiler, Python 3, NumPy, SciPy, and Pillow. Supply the two
original local ZIPs; no LUT images or private manifests are distributed here.

```sh
bash test.sh /path/ULike_Natural_blush_1790815043265.zip \
             /path/ULike_Purity2_1790815028634.zip
```

The tests pin archive and asset hashes before decoding. `test_style_color.cpp`
checks sampler behavior, identity LUTs, floor-slice behavior, interpolation,
alpha preservation, and invalid contracts. `test_actual_assets.py` compares the
actual LUT output with an independent SciPy sampler over seeded samples and
slice boundaries. Temporary decoded fixtures are removed after testing.

Recorded result: **16,421 numeric/contract assertions**, **2,438 actual-asset
cases / 19,504 scalar comparisons**, maximum absolute error
`2.220446049250313e-16`. See `QA_ACTUAL_ASSETS.json` and
`review/INDEPENDENT_REVIEW.json`; the independent review also passed 37 guard
assertions and missing/swapped ZIP rejection.

This validation does not exercise Android, a GPU, active native node ordering,
full-style visual matching, P010 capture, HDR appearance, gainmap packaging, or
the final save path. Build outputs are development executables only and are not
release payloads.
