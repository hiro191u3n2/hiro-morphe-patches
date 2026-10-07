# Q11 / Q13 scope and evidence

## Q11: the actual still conversion boundary

Pinned ULike 5.6.2 `Li/s/a/w/d0/a;->Q0(Image, TotalCaptureResult)`
contains two calls to `Li/s/a/w/s;->u(Image, byte[])`. Both remain after the
v1.9.33 burst entry guard. With burst disabled/rejected, this is the ordinary
still path; the second call delivers the optional still-buffer callback.

The stock `s.u` iterates planes at indices 0 and 2 (increments by 2). It does not
read the U plane. It assumes plane 2 already contains interleaved VU, derives
plane height from pixel stride, copies `ByteBuffer.remaining()` in its tight-row
branch, and consumes the caller's buffers. That layout assumption is incorrect
for valid planar YUV_420_888 and other non-overlapping chroma planes. Padded rows
and shortened final chroma rows can also be copied incorrectly.

`YuvHooks1934` redirects exactly those two invokes, preserving their register
arguments, instruction widths, branches, failure handling, callback and camera
lifecycle. `YuvPlanes1934.copy(Image, byte[])` handles the ordinary still path;
`BurstCapture1933.copyNv21` delegates to the same helper's owned copy. They:

* Read Y, U and V using each plane's actual row stride and pixel stride.
* Pack output as Y followed by interleaved V then U, without interpreting colour.
* Respect each buffer's current position and limit, including a shortened final
  row. Original positions/limits and Image ownership remain unchanged.
* Validate every plane and output length before any destination write.
* Reject unsupported cropped or odd-size frames rather than silently changing
  dimensions/chroma alignment under Q0's unchanged full-frame dimensions.

The next observed path is `TECameraVideoRecorder$60.onPictureTaken` ->
`TEFrameUtils.TEImageFrame2ImageFrame` ->
`MediaRecordPresenter.renderPictureToBitmap` -> native `RecordInvoker` renderer.
The Java `ImageFrame` holder has bytes/planes/format/width/height/rotation, but no
colour standard, transfer or range field. This release preserves sample values
and the SDK's native colour conversion/beauty path. It does **not** assert that
the proprietary native coefficients are verified, does not guess BT.601 versus
BT.709 from resolution, and does not add a second RGB/YUV conversion.

## Q13: exact eligible transforms

The v1.9.33 final quality save path already composed quarter-turn orientation,
center crop and scale through one `FastResize1933.resample` operation. The legacy
`SaveQuality2.normalizePhoto` fallback also uses one composed Matrix draw.
The final HEIF writer uses rotation 0 after normalization. No independent second
save-stage interpolation was found that could safely be removed.

This release preserves that one-pass path and improves its exact integer case:
`FastPixels1933.Plan.exactCrop` detects integer crop boundaries and unit scale.
`runRows` then directly permutes/copies source pixels (quarter turns are read as
virtual rows). It allocates no interpolation axes, row-filter cache or float
accumulators, and `FastResize1933` uses the reduced memory estimate. Fractional
crops and actual scales retain the existing antialiasing interpolation.

This is an exact geometry optimization, not a new beauty mesh implementation.
App-native face/body warps, filter geometry, mirroring policy and preview are not
approximated or suppressed. General beauty warp + resize fusion remains outside
the verified scope because the native warp sampling/coordinates are unavailable.

## Host verification

`host_yuv_geometry1934.py`: 52 scenarios, 2,854 assertions passed, including planar
and shared NV12/NV21 buffers, multiple pixel strides, padding, nonzero offsets,
direct/read-only buffers, shortened final rows, invalid-input atomic rejection,
neutral/primary colour-patch byte preservation, and all quarter-turn/integer-crop
geometry checked against an independent coordinate oracle. The colour-patch RGB
oracle is test-only; it does not assert the camera's actual dataspace.

The existing `host_fast1933.py` suite passed 1,823,698 assertions / 34 scenarios
against the unchanged v1.9.32 pixel reference, including fractional resizing,
alpha fallback, worker failure/cancellation and original-image ownership.

The capture suite passed 516 assertions / 42 scenarios after adding selected
reference index, exact timestamp rejection and canceled-result fallback checks.
Each verifies matching bytes, ShotContext timestamp and SDK metadata timestamp.
These are host fixtures and bytecode checks, not Galaxy/ART device execution.
