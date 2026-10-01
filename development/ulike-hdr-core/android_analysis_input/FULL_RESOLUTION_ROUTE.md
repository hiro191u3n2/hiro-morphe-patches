# 4080×3060 analysis: bounded ownership route

**The bounded streaming ownership candidate is now implemented and host-tested;
it is not enabled or device-validated.**
The legacy 4,194,304-pixel ceiling is our diagnostic guard. It is not an
established limitation of the original SDK, camera or sensor. The source-bound
inventory and reproducible byte calculations are in `CAPACITY_EVIDENCE.json`.

The user previously reported both styles receiving 4080×3060 and completing
3060×4080 beauty output. That supports their earlier app route; it does not
validate this new isolated-recorder/observer path. Their reported downstream
4096 limit covers both sides of this raster, but the reported GL limit was
unavailable. Neither observation proves the actual GL texture limit. No
unscaled 5712×4284 input was established; this proposal targets 4080×3060.

| Legacy boundary before the streaming candidate | Original behavior | Classification |
| --- | --- | --- |
| `AnalysisInput.SDK_MAX_PIXELS`, `Budget`, `prepare` | Reject above 4,194,304 pixels before allocation | Our policy; the constant's name does not establish an SDK capability |
| `StockStillAnalysis.run` and `RenderedDiagnostic` | Repeat the same input/output guard | Our policy |
| `ProbeLedger` | Reject larger grids before native startup | Our policy |
| `OwnedRenderPixels.copy` | Limit and clone diagnostic callback pixels | Our ownership guard |
| `PixelOrientationEvidence.check` | Limit source and callback arrays | Our diagnostic guard |
| `DiagnosticSkinMask.validate` | Allows up to 25 million sampled pixels | Does not block this raster; proves neither native mask resolution nor precision |

The stock Bitmap method forwards supplied dimensions to
`nativeRenderPicture3`. Inspected native instructions at `0x40bdc0–0x40bdc4`
compute `width × height × 4`. The callback path at `0x40b468` computes
`width × height`; its JNI table calls allocate and populate an integer array.
The exact library hash, addresses and independently checked JNI table offsets
are recorded in the evidence file. These inspected slices contain no 4MP
upper check. This is a bounded observation, not a whole-library absence proof
or an execution test.

## Why simply increasing the constant is insufficient

Let **P = 12,484,800 pixels**. Each packed ARGB raster is 49,939,200 bytes
(47.63 MiB); the owned P010 short planes occupy 37,454,400 bytes (35.72 MiB).

The earlier copying path created an analysis integer raster, a Bitmap from its rows,
another Bitmap in `StockStillAnalysis`, and a third in `StockStillFaceProbe`.
That path retained a full integer source raster for orientation comparisons.
The SDK supplied a full integer callback, cloned for diagnostics. The new
candidate eliminates the two downstream Bitmap copies and photographic raster;
the SDK callback and one owned diagnostic remain.

| Known payload at callback | Bytes | MiB |
| --- | ---: | ---: |
| Earlier: P010 + three Bitmaps + orientation raster + callback + owned callback | 337,089,600 | 321.47 |
| Same, if the earlier consumed integer raster has not yet been collected | 387,028,800 | 369.10 |
| Proposed: P010 + one moved Bitmap + callback + owned callback | 187,272,000 | 178.60 |
| Proposed: P010 + one moved Bitmap + callback + owned byte mask | 149,817,600 | 142.88 |

These are calculated payloads, **not measured heap/RSS or sufficient-memory
guarantees**. Bitmap backing/stride, native readback buffers, GPU textures,
models, app baseline, object overhead and allocator behavior remain additional.
Later, `RenderedDiagnostic.pixels()` also clones the retained callback before
`DiagnosticSkinMask.validate` allocates a byte mask. That later copy must be
addressed separately.

## Implemented ownership patch

1. Added an exclusive Bitmap submission envelope with states
   `CREATED → TRANSFERRED → SUBMITTED → CLOSED | QUARANTINED`. It renders immutable
   `SdrRendition` rows directly into its one Bitmap while computing the existing
   exact proxy digest. Frame/settings/nonce/grid provenance is unchanged.
2. Added ownership-transfer overloads through the analysis and probe layers,
   eliminating both subsequent Bitmap copies. Retain the copying API for
   callers without exclusive ownership. **Only the probe may recycle a moved
   Bitmap after verified native joins.** Outer `finally` blocks must not recycle
   transferred storage; failed teardown must retain it in quarantine and prevent
   subsequent native work.
3. Full source-image orientation comparison remains a separate legacy diagnostic mode.
   The mask path can validate its explicit complete UV ramps and callback
   interpretation without retaining another full photographic raster. Never
   infer or silently correct native geometry from this change.
4. The unavoidable current SDK integer callback remains borrowed. A single
   owned integer copy has a consuming conversion API; native-size `.pixels()`
   rejects another full clone. A binding can call `DiagnosticSkinMask.validate`
   inside `consume` to produce byte[P] directly. The borrowed owned array is wiped
   on success/failure; no SDK array escapes its callback lifetime.
5. An explicit `AnalysisCapacity` policy is threaded through input, probe,
   ledger, callback and diagnostic guards. The legacy 4MP policy is unchanged.
   A separate 4080×3060 candidate passes full-grid row/digest tests and host
   ownership/callback/cancel/quarantine checks. Independent Python verifies both
   digests after an actual 12.5MP stream under a 64MiB host heap. This excludes
   Android Bitmap/native/model allocations; device calibration and measured
   total-capacity checks remain required.

Full-grid diagnostic sampling is still 8-bit and can sample a lower-resolution
native segmentation texture. It must not be presented as native 12.5MP
segmentation, exact mask readback, completed HDR integration, or device support.

The current callback payload formula includes an additional 4×width-byte row:
187,288,320 bytes for the proposed owned-ARGB route. A consuming mask conversion
can temporarily hold the owned ARGB and byte mask together, after the native
Bitmap/worker teardown; total process peak still needs device measurement.
