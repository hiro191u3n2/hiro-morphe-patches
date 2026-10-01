# P010 input boundary

This is an inactive source component for a new HDR engine. It does not open a camera, configure a
session, capture a photo, call ULike's native beauty engine, encode a file, or change an APK/MPP.
It does not assert actual P010 delivery on the SM-S948Q or native 24.5 MP support.

## Contents

- `src/main/java`: Android-independent `HdrFrame`, `P010FrameReader`, `CaptureMatch`, `InputRejected`.
- `src/android/java`: `AndroidP010FrameReader`, requiring API 33+ for actual Image dataspace.
- `src/test/java`: core tests using padded rows and overlapping physical chroma storage.
- `src/test-android/java` and `src/test-stubs/java`: host doubles testing adapter decisions and
  close responsibility. The doubles are excluded from the real Android SDK compilation.

## Supported input and output

The input is Android P010 format 54 with **three logical planes: Y, Cb and Cr**. The physical
semiplanar layout does not mean that `Image.getPlanes()` returns two planes. Each logical plane
is read from its own view; an extra Cr offset must not be added.

This initial subset requires:

- Positive, even, exact negotiated width and height. No resize or orientation transform occurs.
- A full-frame crop `(0, 0, width, height)`. Cropped Images are explicitly rejected, including
  otherwise valid even crops. This avoids an implicit crop/chroma-siting change.
- Y pixel stride 2; Cb and Cr pixel stride 4; positive even row strides; equal chroma row strides;
  rows large enough for complete physical CbCr pairs. Other layouts are rejected.
- The final requested sample, not all final-row padding, must exist within each buffer's limit.
  Long arithmetic checks addressing and sizes before allocation.
- Explicit BT.2020 **non-constant-luminance** YCbCr, HLG transfer, and FULL or LIMITED range.
  UNKNOWN, unspecified range, BT.2020 constant-luminance, PQ, SDR and unexpected range are rejected.
  Actual Image dataspace must match the caller's negotiated `Request.encoding`.
- Each sample is a little-endian 16-bit P010 word with its low six bits zero. Nonzero low bits
  reject the input; the reader does not mask malformed samples into apparent success.

The output owns Y/Cb/Cr `short[]` sample storage. It exposes only read-only `ShortBuffer` views.
Each sample is a positive 0..1023 code value. The lossless `word >>> 6` unpack removes only the
validated six zero bits. Chroma stays 4:2:0; no chroma interpolation, gamut conversion, HLG EOTF,
tone mapping, clipping, normalization or compression is applied. LIMITED-range headroom and
footroom code values are preserved rather than clamped to nominal video ranges.

This preserves delivered 10-bit code values; it does not prove sensor bit depth, scene dynamic
range, ISP quality, or that a producer did not expand an earlier 8-bit image into P010.

## Same-frame and lifecycle contract

`CaptureMatch.Context` must be captured at submission with exact owner, session, reader,
request and callback object identities, a non-reused shot number, logical camera ID and optional
explicit physical output binding. `Source` is captured when the reader is installed. Never
construct these from a newer current shot to label an old callback.

The adapter checks the actual `TotalCaptureResult.getRequest()` object identity against the
submitted request. Logical output uses its total result's `SENSOR_TIMESTAMP`. A physically bound
output requires the specifically bound ID in `getPhysicalCameraResults()` and uses that result's
timestamp. An active physical ID reported for a logical output is recorded separately; it is not
silently substituted as an explicit output binding. Exposure time and sensitivity are nullable
metadata from the same chosen result.

Image and result timestamps must be positive and exactly equal. The source must explicitly declare
`SENSOR_START_OF_EXPOSURE`: the future session owner must establish a sensor timestamp base and
disable readout timestamps. This module does not set those output options. Other time bases,
readout timestamps, partial/missing results and nearest-frame matching are unsupported.

The live selection is checked before and after copying. The caller must serialize Image access,
session changes and committing the result, and recheck ownership at an asynchronous consumer's
commit boundary. These checks do not make arbitrary concurrent close/switch/use safe. An Android
Image exposes no public original reader/request identity; provenance must come from the registered
reader callback and submission callback, not a caller-supplied guess.

`InputRejected.reason` reports the rejection category. A rejection must not become a successful HDR
save through a silent Bitmap/NV21/JPEG fallback. Exceptions do not complete ULike callbacks or
restore preview themselves; the future integration remains responsible for exactly-once completion
and preview restoration for the matching shot.

## Memory and close responsibility

The mandatory caller `Request.maxAllocationBytes` limits the **total sample-array payload** to
`width * height * 3` bytes. For 4080 x 3060 this is 37,454,400 bytes. VM object/array headers,
ByteBuffer view objects, the source Image, other in-flight frames and later processing buffers are
not included. The caller must leave headroom and independently bound concurrent copies. There is
no retry with a smaller/rescaled/8-bit allocation. Java allocation failure remains an error.

- Core `copy` borrows buffers synchronously and never closes or changes their cursors. Their
  positions denote each logical plane's origin. Buffers must stay alive and unmodified during copy.
- Android `copyBorrowed` keeps caller ownership on success and failure. The caller must close the
  Image. Native Image planes must be untouched (`position == 0`); consumed cursors are rejected.
- Android `copyAndClose` takes exclusive ownership at entry and closes once using try-with-resources,
  including rejection or allocation/runtime error paths. The caller must not close it again. If
  close fails during rejection, the original rejection survives and close is a suppressed exception.
- The returned `HdrFrame` has no Image/plane-buffer alias and needs no close; its immutable Java
  storage is reclaimed when the frame and its read-only views are no longer referenced.

## Tests

Run `bash test.sh`. The script supports either `javac` or the local JDK compiler module invoked
through `java -m jdk.compiler/com.sun.tools.javac.Main`. It compiles against the Java 8 language/API
target. Set `ANDROID_JAR` to a real Android API 36 SDK `android.jar` to additionally compile the
production core and adapter against that SDK. If omitted, the script explicitly reports that SDK
compilation was not run. Host doubles are not HAL/framework execution.

Tests cover native dimensions and all ten bits, padded/shared chroma views, absent last-row padding,
immutable owned output, malformed plane extents/strides/low bits, allocation/overflow rejection,
range/crop rejection, every capture identity, stale selection during copy, exact timestamp and
physical-result provenance, borrowed/owned close behavior and exceptions during close.

## Remaining integration

No camera-session hook or live-preview change is included. Advertised P010/HLG10 support does not
prove the desired stream combination or a delivered frame. A future integration must validate that
combination, preserve existing AF/AE, obtain an actual P010 frame and its dataspace/result, implement
the new high-precision beauty/render and encoder pipeline, and validate the resulting file. The
separate C++ float RGB LUT/gainmap work is not yet connected to this module through JNI or an HLG /
BT.2020 conversion. Model weights and full vendor-style reproduction are not provided here.

## Primary contracts consulted

- https://developer.android.com/reference/android/media/Image#getFormat()
- https://developer.android.com/reference/android/graphics/ImageFormat#YCBCR_P010
- https://developer.android.com/reference/android/media/Image.Plane#getBuffer()
- https://developer.android.com/reference/android/media/Image#getDataSpace()
- https://developer.android.com/reference/android/hardware/DataSpace
- https://developer.android.com/reference/android/hardware/camera2/params/OutputConfiguration#setTimestampBase(int)
- https://developer.android.com/reference/android/hardware/camera2/params/OutputConfiguration#setReadoutTimestampEnabled(boolean)

These contracts and host tests are not measurements of the user's phone.
