# Standalone Android P010/HLG still source

Source component only. This module has **not been installed in ULike or exercised on a phone**.
It does not replace the existing 8-bit beauty path and does not prove end-to-end HDR or native 24.5 MP.

`HlgStillSource.configure(...)` creates a normal, single-output API 34+ Camera2 session for an
already opened, exclusively owned camera. It never opens a hidden camera ID. The caller supplies
a Handler, exact output width/height and owned-sample byte budget. A physical route must be a
member of the logical camera, and both logical and physical capability/color declarations are checked.

The source requires P010, HLG10 and the BT.2020 HLG color tuple; checks normal regular/high-resolution
P010 dimensions; and asks the device about this exact session combination before creating it.
An unsupported/unknown session query is rejected. There is no JPEG, 8-bit, maximum-sensor-mode or
resolution fallback. Requesting 5712×4284 against the supplied 4080×3060 capabilities is rejected.
The negotiated output dimensions alone do not prove a sensor's photosite resolution.

Output timestamps use `TIMESTAMP_BASE_SENSOR` and start of exposure, with readout timestamps disabled.
The output is configured without mirroring, and each request sets rotate-and-crop to NONE. Actual
rotate-and-crop and sensor pixel mode metadata are checked before a frame is eligible. The intended
display rotation is separate metadata and never silently rotates or mirrors the owned P010 samples.

Only one still can be in flight. The gate joins the exact submitted request with a full capture
result and exact `Image.timestamp == SENSOR_TIMESTAMP`; it never uses a nearest timestamp or preview
face landmarks. For a selected physical route, the timestamp/crop/exposure metadata comes from that
physical result. Every acquired Image has one owner; it is closed after its 10-bit samples have been
copied, or on duplicate, mismatch, timeout, cancel or shutdown. Late results cannot become metadata
for a newer request. A late old Image can cause the new shot to be rejected; it cannot be paired with
an unequal timestamp.

The existing `hdr_input` adapter validates actual Image dataspace, full Image crop, strides, bounds,
low P010 padding bits and allocation budget. Full/limited range is taken from the actual Image, not
guessed from the requested profile. The returned `CapturedStill` owns immutable 10-bit sample planes
and includes session UUID, request sequence, sensor timestamp/frame number, physical route, exposure,
sensor crop, image crop, camera orientation, applied pixel transform and separately intended display
rotation. Later RGB proxy/face analysis must bind to this exact frame and map landmarks back to its
pixel grid. RGB conversion and face analysis are not part of this source.

## Scope and lifecycle

- All public calls and callbacks must run on the supplied serialized Handler.
- Creating the session replaces any existing session on the supplied CameraDevice. This is a
  standalone source, not a safe drop-in change to ULike's current camera controller.
- This version uses one P010 output, with no simultaneous preview, recording, extension, reprocess,
  burst, high-speed or maximum-resolution mode. It provides no 3A convergence/flash controller,
  manual exposure inheritance, focus-speed improvements or lens-switch performance claim.
- Call `capture(displayQuarterTurns, timeoutMillis)` only after `onReady`. The still template's
  defaults remain in use. A timeout is bounded to 100–30000 ms.
- `close()` closes this session, reader and retained Images, but never closes the borrowed CameraDevice.
  The caller must stop this source before lending that device to another controller.
- The payload budget covers the copied sample planes. It is not a guarantee of total HAL/Java heap
  use; two native ImageReader buffers and other application allocations also consume memory.
- Actual HAL configuration, callback ordering, camera geometry, P010 color data and phone rendering
  still require instrumented device tests. Host tests and SDK compilation cannot establish these.

## Verification

Run `ANDROID_JAR=/path/to/android-36/android.jar bash test.sh`. The Android-independent gate tests
exercise both arrival orders, request identity, timestamp mismatch, duplication, late callbacks,
close failure, cancellation, no-upscale selection and profile constraints. Android source is compiled
against the real SDK 36 API; it is not executed by the host suite.

Official API references consulted on 2026-10-01:

- https://developer.android.com/reference/android/hardware/camera2/params/OutputConfiguration
- https://developer.android.com/reference/android/hardware/camera2/params/SessionConfiguration
- https://developer.android.com/reference/android/hardware/camera2/params/DynamicRangeProfiles
- https://developer.android.com/reference/android/hardware/camera2/params/ColorSpaceProfiles
- https://developer.android.com/reference/android/hardware/camera2/CameraDevice

The original ULike APK and complete disassembly are not included in this module.
