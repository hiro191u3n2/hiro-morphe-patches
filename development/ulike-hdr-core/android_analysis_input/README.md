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

The current diagnostic bridge imposes a conservative guard of 4,194,304 pixels.
This is our code's present bound, not an established native SDK or sensor limit.
This module retains that guard and rejects a 4080×3060 frame before allocating
analysis pixels. It does not silently make a smaller proxy or claim that native
12.5MP processing is complete. Raising or replacing this limit requires a
reviewed memory/lifecycle path and real-device calibration, not a larger UI
resolution setting. No Android device was run by the host verification.

`verify.py` compiles against SDK36, creates DEX, checks ownership/failure paths
with actual immutable HdrFrame/SdrRendition objects, and independently computes
all 3,072 fixture pixels and both hashes in Python. Tests include settings
mutation, metadata/pixel changes, interrupted transfer, failed begin/row/finish,
cleanup failure, reentrant use, repeated use, exact allocation bounds and the
actual reported 4080×3060 input rejection. All fixture pixels are synthetic.
