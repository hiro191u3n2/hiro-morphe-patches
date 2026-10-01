# Android-compatible Main10 HLG HEIF mux

`Main10Heif` creates a real single-image HEIF file from validated Main10 HEVC.
It is Java 8 source compiled against SDK 36 and uses Android-compatible Java APIs.
`fromEncoder(AndroidMain10Encoder.Result, Crop)` connects directly to the
P010/Main10 encoder module. No platform HEIF writer, JNI library, JPEG encoder,
8-bit Bitmap, or decoding/re-encoding is used by the mux.

The file contains `heix`/`mif1` brands, one primary `hvc1` item, `hvcC` VPS/SPS/PPS,
four-byte length-prefixed sample NALs, explicit 10-bit `pixi`, BT.2020 / HLG /
non-constant-luminance `nclx` with the original full/limited range, `ispe`, and
file-relative `iloc` extents. Optional `clap` expresses an integer crop with exact
signed rational offsets. This allows odd display dimensions from an even 4:2:0
encoded image without enlargement. Encoded odd 4:2:0 dimensions are rejected.
Crop left/top must be even to preserve the 4:2:0 chroma phase. Odd origins are
rejected explicitly; coordinates are never silently rounded. Odd crop widths
and heights remain supported at an even origin.

Preparation clones the input and validates it before producing output. It checks
actual SPS dimensions/color/depth, all accepted NAL emulation bytes, restricted
single-layer VPS and PPS syntax, VPS→SPS→PPS→IDR slice references, and one first
slice marker. Coded slice entropy and the complete SPS extension grammar are not
decoded here. This is not a general-purpose HEVC conformance validator. Unsupported
VPS HRD, multilayer sets, VPS/PPS extensions, non-IDR images or changing parameter
sets fail explicitly instead of being passed through as known-compatible data.

`Prepared.writeTo` writes to an **empty, caller-owned output stream**. It never
closes or flushes that stream. The caller must discard partial bytes on I/O
failure; absolute item offsets require the HEIF to start at byte zero.
`Prepared.saveAtomic` writes and fsyncs a temporary file in the destination
directory, then atomically replaces the target. There is no non-atomic fallback.
Its file path API needs Android API 26+, while the encoder requires API 33+.
This is atomic file replacement, not a guarantee of directory-entry durability
after sudden power loss.

## Verified

```sh
python verify.py --android-jar /absolute/path/android.jar \
  --jdk-bin /absolute/path/jdk/bin --report QA_ANDROID_HEIF.json
```

The verifier compiles the actual production sources with SDK 36 and generates
independent x265 fixtures. It writes five HEIF files: full/limited 64×32,
conformance-cropped 66×34, a 65×33 clean aperture, and an offset 63×31 crop.
Libheif 1.23.2 independently decodes them as actual 10-bit Y/Cb/Cr. Every sample
matches the FFmpeg decode of the original HEVC (or the requested crop) exactly.
This proves the mux preserves the **already encoded** pixels; HEVC VBR itself
is still lossy. Independent box/offset/property checks and Java failure cases
are recorded in `QA_ANDROID_HEIF.json`.

Libheif's default decoding settings convert an unspecified output NCLX to sRGB.
For this HDR-preservation test the verifier explicitly enables version 10
`output_image_nclx_profile_passthrough`, disables 8-bit HDR conversion, requests
native color/chroma, and enables strict decoding. It requires a decoder exposing
that options version. Simply requesting 10-bit output would not avoid the color
conversion.

## Remaining scope

No Android device executed this module or the preceding MediaCodec boundary.
No phone/gallery HDR rendering, app hook, MediaStore publication, EXIF/orientation
preservation or full beauty pipeline is claimed. The caller must supply correctly
oriented processed pixels and manage final app storage.

This is **direct HLG10 HEIF, with no gainmap/tmap**. A gainmap requires a separately
established processed HDR/SDR pair, a decoded-base identity, appropriate base/map
encoding, and metadata/reconstruction checks. Those cannot be invented from one
HLG frame. The existing host `heif_save` component tests a separate gainmap path;
it is not yet connected to this Android-compatible mux.

References: Android APIs and official library pins are in
`../android_encoder/OFFICIAL_SOURCES.json`. The exact decoder option contract is
the official libheif v1.23.2 `libheif/api/libheif/heif_decoding.h` and
`heif_color.h` at https://github.com/strukturag/libheif/tree/v1.23.2/libheif/api/libheif.
