# Android P010 → Main10 encoder boundary

This source module implements a strict API 33+ `MediaCodec` adapter for one
BT.2020 non-constant-luminance HLG P010 image. It returns an Annex B HEVC picture.
It does **not** write a HEIF file, attach a gainmap, perform beauty inference,
convert processed RGB to HLG, install an app hook or add a settings screen.

`P010Frame` owns copied 10-bit codes, immutable dimensions/color/range, and
capture/geometry/processing provenance IDs. Its writer honors each Image plane's
position, limit, row stride and pixel stride. All layouts are checked before any
plane is changed. Codes are written in the high 10 bits of little-endian words;
there is no byte RGB/YUV staging or JPEG.

`Options` exposes `HARDWARE_ONLY`, `HARDWARE_PREFERRED`, and `ANY` selection plus
an exact codec name. Selection requires explicitly advertised P010, Main10,
requested dimensions/format/bitrate and VBR. An eligible software Main10 encoder
may be selected with `HARDWARE_PREFERRED` only when no hardware candidate qualifies.
There is no 8-bit fallback. Hardware is a manufacturer-supplied attribute; this
does not identify the encoder or processing used by Samsung's camera application.

`AndroidMain10Encoder.encode` is a blocking worker-thread operation. It obtains
the actual writable P010 `Image`, copies all components, queues one frame with
EOS, drains output, checks returned color metadata, then inspects actual VPS/SPS/
PPS presence, Main10/4:2:0/10-bit, cropped dimensions, HLG/BT.2020/full-or-limited
VUI fields and one IDR picture. Dequeue loops have a 30-second deadline and check
thread interruption. Android configure/start/stop calls are platform operations
and cannot be made time-bounded by this Java method. Unsupported layouts, absent
metadata, non-Annex-B output and unadvertised combinations fail explicitly.

`HevcProof` is a bounded **contract inspector**, not a complete HEVC decoder. It
parses SPS through its VUI color description and counts the first-slice markers;
it does not validate all later SPS extensions, parameter references or coded
slice payloads. Returning a Main10 SPS does not prove the encoder retained every
input distinction, HDR appearance, or image quality. VBR HEVC is lossy.

## Validation

Run with a real SDK 36 `android.jar` and JDK:

```sh
python verify.py --android-jar /absolute/path/android.jar \
  --jdk-bin /absolute/path/jdk/bin --report QA_ANDROID_ENCODER.json
```

The script compiles every Android adapter source against the real SDK (Java 8
bytecode), generates independent x265 full/limited HLG Main10, SDR8 and PQ10
fixtures, verifies their tags with ffprobe, decodes them with FFmpeg, and runs
2,392 Java checks. Checks cover all 1,024 P010 values, low six bits, padded and
shared chroma views, chroma row overlap rejection, immutable source ownership, validation before mutation,
policy/name/precision selection and real-bitstream rejection cases.

No Android device was available for this validation. Device enumeration,
configuration, P010 image delivery and encoding remain unexecuted. Before app
integration, exercise these paths on SM-S948Q and independently decode the actual
device output; compare pixel/color/range and precision test patterns. Add a HEIF
container and compute/encode its gainmap against the decoded base image. The
existing `heif_save` host component demonstrates that container/math path but
is not connected to this Java encoder.

## Why the existing HEIF helpers were not relabeled 10-bit

The downloaded official AndroidX HeifWriter 1.2.0-beta01 source still constructs
the HEIF encoder with `useBitDepth10 = false`; its AVIF path differs. Official
libultrahdr v2.0.2's HEIF `fill_img_plane` allocates 8-bit planes and copies
8-bit data for the base/gainmap. Merely changing their declared depth would
mislabel pixels. These exact artifact hashes and the public Android APIs used
here are recorded in `OFFICIAL_SOURCES.json`. Future library versions must be
audited independently.
