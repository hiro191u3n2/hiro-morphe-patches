# Same captured still: analysis input provenance

`AnalysisInput.prepare` renders the actual immutable captured P010 frame through
the same `SdrRendition` used for neural inference. Only the separate native
analysis raster is quantized to opaque encoded sRGB8. The original HDR samples
and continuous neural input are unchanged. No JPEG, resize, rotation, mirror or
crop is introduced here.

The owned raster records the exact Java frame identity, sensor timestamp,
geometry identity, rendition policy and hashes of every source sample and the
exact application-settings bytes. Its proxy digest uses the existing native
bridge's `ULSP` version 1 format: dimensions followed by big-endian opaque ARGB
pixels. The separate `ULHF` version 1 digest includes capture metadata and every
10-bit code in canonical component order; it does not replace object ownership
or a calibrated camera/native coordinate contract.

`Owned.transfer` allows one complete row transfer. Success, exception and
interruption all consume and clear the owned raster. Borrowed row buffers are
also cleared. Failure aborts the destination; cleanup exceptions do not replace
the original failure. Budgets cover this module's retained integer raster plus
its transfer row, not the native SDK, Android Bitmap copies or total app heap.

`AndroidAnalysisInput.run` creates the actual Bitmap and request, invokes
`StockStillAnalysis.run` under the caller's real idle SDK lease, then checks
request-object identity, submitted proxy digest, native callback dimensions,
nonce and completion. A successful `BoundOutcome` establishes that the submitted
analysis pixels came from that captured rendition. It deliberately does not
certify native coordinates, mesh routing, chroma siting, mask precision, renderer
ordering or original style equivalence. Setup still requires a complete replay
of the captured composer state; no guessed setup or license bypass is provided.

The copying `prepare`/`Owned` API retains its legacy 4,194,304-pixel guard.
That is application policy, not an established native SDK or sensor limit.
The new `prepareStreaming(..., AnalysisCapacity.nativeSize4080x3060Candidate())`
preflights the actual input grid and a known-payload policy before native startup.
It streams every captured-rendition pixel directly into one Bitmap, computes the
same ULSP digest, and moves that Bitmap through `StockStillAnalysis.runOwned`
and `StockStillFaceProbe.runOwned` without downstream Bitmap copies. Its
`Streaming.descriptor()` becomes available after successful row transfer.
No full photographic int[P] orientation raster is allocated on this route;
orientation calibration remains explicit and unresolved.

`OwnedBitmapSubmission`/`SubmissionOwnership` enforce one transfer and one native
submission. Closing an outer owner after transfer cannot recycle native input.
Only a successful native stop/unregister/uninit sequence releases that Bitmap;
failed cleanup retains it, callbacks and lease in process quarantine and blocks
another probe. A callback image is copied at most once after exact grid and
submission checks. Duplicate/early callbacks fail before a second copy.

`RenderedDiagnostic.consume` lets the binding validate the sole owned int[P]
array directly into `DiagnosticSkinMask`'s byte[P], then wipes/releases the
integer array on success or failure. Its borrowed array must not escape the
consumer. Native-size `.pixels()` deliberately rejects, preventing an accidental
second full ARGB clone; `close()` discards unused diagnostics. This is still an
8-bit sampled diagnostic, potentially from a lower-resolution native mask.

The explicit candidate accepts 4080×3060 and its transposition, at most
12,484,800 pixels with either side at most 4080. Its calculated known callback
payload is 15P + one row = 187,288,320 bytes, covering P010, one Bitmap, one SDK
callback and one owned diagnostic. Baseline app, native/GPU/model memory,
allocator behavior, Bitmap stride and extra callback activity are additional;
this is not a total-heap admission guarantee or device capability claim.
A 5712×4284 raster remains rejected, and no interpolation manufactures detail.

`verify.py` compiles against SDK36 and creates DEX. It independently compares
all 3,072 nonuniform fixture pixels and hashes in Python, then streams all
12,484,800 pixels of a second fixture under a 64MiB **host** Java heap and
independently verifies both full-grid digests. This latter test covers P010 and
row rendering only, not Android Bitmap/native/model memory. Separate host checks
exercise transfer/release/quarantine, failure/cancellation/alias handling,
diagnostic consumption, and full-grid callback geometry. No Android device was
run and native geometry, completion order and actual capacity remain unverified.

Ownership cleanup correction (2026-10-03): `BoundOutcome` and the lower
`Outcome` are now closeable. If the preview/composer restoration fails after
a completed analysis, `PausedStockPreview` closes its undelivered outcome,
wiping the full diagnostic raster. A restoration `Error` is handled with the
same cleanup guarantees as an exception; original failure identity is preserved
if cleanup also fails. The legacy input adapter now closes a diagnostic rejected
by its provenance checks as well. Both adapters mark an outcome delivered only
after its wrapper allocation succeeds. Native input quarantine rules are unchanged.

The SDK callback also now wipes a newly owned raster if its observer rejects
ownership, without modifying the borrowed SDK array. This fixes failure-path
retention of a potentially 49,939,200-byte diagnostic; it does not turn
full-resolution host checks into phone memory or native coordinate validation.
