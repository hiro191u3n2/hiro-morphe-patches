# File-backed HDR photo transaction

This module connects the actual `HdrBeautyProcessor.PairSink` to private FP64 pair storage,
integer output geometry, the verified `GainmapSave` encoder path, and a concrete Android
MediaStore publisher. It does not invent an HDR companion from an unprocessed original HDR
plus an edited SDR image.

Production entry:

```java
// On an app worker thread. Run recover(context) once during startup before accepting shots.
try (AndroidPhotoTransaction photo = AndroidPhotoTransaction.begin(context, identity, diskBudget)) {
    GeometryPairWriter pair = photo.staging.createTransformedPair(geometry, headroom, tileBudget);
    HdrBeautyProcessor.render(snapshot, sameStillBindings, pair.asHdrSink(), processingBudget);
    Uri completedPhoto = photo.savePair(pair.pair(), qualityLimits, androidGainmapCodec);
    // Pass this completed URI to the app. No preview JPEG/NV21 callback is used.
}
```

`PhotoIdentity` is built from the exact shutter-time source token, capture identity, planned
integer geometry, processing revision, and a defensive copy of the settings snapshot.
For the HDR processor, use `snapshot.settingsSnapshotBytes()` so the digest is exactly the
one echoed by `PairSink.begin()`. The snapshot geometry ID must be `geometry.id()` and the
photo identity dimensions must be the final crop dimensions. A settings/source/geometry
or headroom mismatch fails before accepting output rows.

`StoredRgb` supports FP32 and FP64 storage with explicit encoded-sRGB or display-linear
BT.2020 domains. `asBeautySink()` accepts the actual `BeautyImageEngine.TransactionalSink`
calls and seals a **private** encoded FP32 intermediate. `PairedStore` accepts both processed
display-linear FP64 renditions, in 203-nit SDR-white units, and seals both together. Neither
of these commit operations publishes a photo.

Each staged file has a bounded versioned identity header, full row hashes, and hashes for
64-pixel blocks. Readers verify complete extents, identity/header, checksum indices and the
data they read. A four-row cache supports the neural crop input. Arrays are borrowed only
for the duration of a write and are copied to disk before returning. No public writable
pixel buffers or file paths are returned.

`GeometryPairWriter` stages the processor's source raster and then physically applies the
same clockwise 0/90/180/270-degree rotation, optional upright horizontal mirror, and explicit
upright crop to **both** processed renditions. It does not resize or interpolate. The 90-degree
path reads short verified spans; it does not reread a full row for every rotated pixel.
The exact mapping is tested for every rotation/mirror combination, including non-block-aligned
crops, and all output FP64 samples must match their source samples bit for bit.

Memory and storage budgets are explicit:

- Transform tile arrays are bounded by `48 * outputWidth * tileRows + 48 * tileRows + 16384`
  bytes, with at most 64 tile rows. The caller supplies the limit; too-small limits fail.
- A staged raster uses `4096 + width * height * 3 * bytesPerSample +
  height * (1 + ceil(width / 64)) * 32` disk bytes. The transaction reserves a cumulative disk
  budget before creating each file and checks currently available space.
- Checksum indices occupy the same footer byte count in Java arrays, plus array headers.
  Cached decoded rows use at most `4 * width * 3 * 8` bytes per reader. These are separate from
  the transform tile arrays. Source-pair readers and checksum arrays are released after a
  successful orientation/crop. The model, P010 codec, HEVC bytes and other modules have their
  own allocations; this is not proof of a 32 MP phone heap budget.

For a 4080 x 3060 capture, one FP64 pair occupies 612,008,192 bytes. An uncropped 90-degree
output pair occupies 612,073,472 bytes, so source and destination staging can coexist at
about 1.14 GiB before the source pair is removed. The caller must budget that private disk
space as well as the separate encoder/container scratch, even though the transform arrays
are bounded.

The final publisher uses an app-private persistent journal and an exclusive transaction lock:

1. Complete HDR appearance processing, stage the pair, apply exact geometry, and pass the
   actual-decoded base/map quality gates before creating any MediaStore item.
2. Persist the unique planned filename, then create an owned `image/heic` item in
   `DCIM/Camera/` with `IS_PENDING=1`.
3. Write the complete verified HEIF through a counted stream, fsync it, and check descriptor
   byte length and ownership. Cancellation or write failure leaves it pending for cleanup.
4. Set `IS_PENDING=0` for that exact owned pending row and confirm it. Only then return its
   URI. The random filename does not overwrite an existing photo.

Immediately before step 4, the transaction persists `PUBLISHING`. Once the provider call has
been attempted, the same transaction cannot stage or save again. If the confirming read
throws, reports another URI/owner/identity, or disagrees with the verified byte count,
`PublicationUncertainException` carries the **unconfirmed** candidate URI, transaction ID,
identity digest and expected bytes. This outcome must not trigger a save-success callback,
an ordinary unsaved-photo error, or an automatic retry. Closing an uncertain transaction
retains its journal and all media. `PhotoTransaction.reconcile(store, exception)` performs
only an exact read: it returns `PUBLISHED` or `PENDING` when that can be verified and otherwise
keeps reporting uncertainty. It never republishes or deletes a candidate. Reconciliation
does not turn a still-pending item into a successful save.

Startup recovery handles the process-crash gap between insert and recording the returned URI
by finding the exact planned UUID filename/path owned by this package. It removes only owned,
matching, still-pending rows. Already-visible completed photos are preserved even if the last
journal still says INSERTED or PUBLISHING. Active locks are skipped. Corrupt journals are retained and
reported after independent valid transactions have been recovered; their media is not guessed
or deleted. Internal cleanup validates contents first and deletes the journal last. This is
process-restart recovery, not a claim of power-failure-proof durability on every filesystem.

The production Android backend is compiled against SDK 36 and D8. Host tests exercise real
row files, the actual Main10 encoder/container path, completed HEIF decoding with libheif,
injected I/O/cancellation failures, and separate JVM processes killed after insert, after
write, and after publication. Five additional cases exercise confirming-query I/O/access
failures, changed URI/byte count and an unconfirmed still-pending item. They verify one
publication attempt, preserved media/journals, read-only reconciliation and later recovery.
The persistent host gallery models MediaStore's pending state;
it is explicitly not an Android MediaProvider execution. Device/gallery execution, app UI
completion callback integration, native ULike style binding completeness, and EXIF retention
remain separate checks.

With the optional local model inputs, the author verifier also executes the pinned
`tt_baoman` and `tt_goodlike` models with their real projected masks. Each receives a smooth
synthetic 512 x 384 P010 capture; the remaining resolved style inputs are explicit synthetic
fixtures, not a claim of complete native ULike style binding. Both routes produce a real
382 x 508 HEIF after 90-degree rotation, upright mirror and crop. Both encoded items are
decoded by libheif and reconstructed using the metadata actually stored in the file.
The recorded maximum HDR errors are about 0.01148 and 0.01412, in 203-nit-white units.
Processed and reconstructed HDR peaks exceed SDR white in both cases (about 3.3163 and
2.1148 respectively), so these cases test actual highlight-bearing output.
The unchanged quality limits are maximum SDR 0.2, maximum HDR 0.5, and HDR RMSE 0.05.

Main10 4:2:0 is not lossless RGB. A sharp synthetic HDR fixture can fail the unchanged quality
gate despite lossless HEVC code encoding. Such failure must keep the photo unpublished; it
must not be hidden by silently weakening the quality limits.
The retained sharp actual-model fixture has maximum HDR error about 0.79463 and is therefore
rejected before any pending gallery row is inserted. These tests demonstrate both a successful
end-to-end output and a genuine limitation of the selected 4:2:0 representation.

Run `verify.py` with the SDK36/JDK21/ORT classes and D8 paths. Supply all five optional flags
`--models-dir`, `--stock-libs`, `--natural-zip`, `--purity-zip`, and `--ort-host` to include the
actual-model cases. No model weights, proprietary libraries, archives or output portraits are
bundled by this module.
