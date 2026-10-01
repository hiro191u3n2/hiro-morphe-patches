# Host RGB10 HEIF + ISO gainmap save path

This directory implements a **host reference writer**, not an Android camera
feature or an AI beauty renderer. It accepts an already processed, matched pair
of linear HDR and SDR RGB arrays. The SDR primary and RGB gainmap are actually
encoded as **10-bit HEVC 4:4:4 Range Extensions**, without a JPEG stage. No
8-bit intermediate or silent codec fallback is used.

## Implemented

1. Validate equal dimensions, capture/geometry/processing IDs, known BT.2020
   primaries, 203-nit SDR reference white, FP32/FP64 nonnegative values, and
   explicit display headroom. The IDs are caller assertions, not image analysis.
2. Make private copies of the processed pair and revalidate their values. The
   caller must synchronize the pair and not mutate it during the save call;
   two copies cannot atomically snapshot concurrent external writes. Apply
   the sRGB transfer curve to the SDR rendition
   in BT.2020 primaries and quantize to 1024 RGB levels. Out-of-range values are
   rejected rather than clipped. Tone mapping is the caller's responsibility.
3. Encode the base losslessly with FFmpeg/libx265, verify actual HEVC profile,
   dimensions and color VUI with ffprobe, and decode it again. All RGB10 codes
   must match. The SDR rendition is 10-bit; it is **not a direct HDR primary**.
4. Compute the gainmap against the **actual decoded and linearized base** and
   processed HDR. This links the existing `../gainmap` C++17 FP64 math without
   modifying it. It avoids retaining a pre-beauty map or a map derived from a
   different base quantization. Quantize the numerical map to RGB10 and encode
   it losslessly, with another mandatory exact-code decode check.
5. Serialize a restricted ISO 21496-1 version-0 metadata payload using separate
   rationals, SDR-base direction, 3 channels, base color space, positive encoding
   gamma, linear offsets and log2 headrooms/bounds. Validate rational precision
   after rounding. Unrepresentable headroom/gamma/range is rejected.
6. Write an actual HEIF with HEVC `hvc1` items, parameter sets in `hvcC`, length
   prefixed NAL data, `heix/mif1/tmap` brands, hidden gainmap, `tmap` item,
   `dimg` references `[base,map]`, and `altr` preference `[tmap,base]`. The normal
   base remains primary. A completed file replaces the destination atomically.

Color labels: base BT.2020 / sRGB transfer / identity RGB / full range; gainmap
BT.2020 / linear / identity / full range. Gainmap values are numerical and must
not undergo photographic transfer or color-space conversion. The derived tmap
rendition declares BT.2020/PQ/identity/full range; applying the map yields linear
HDR in 203-nit units, which a renderer must convert to the PQ output encoding.
The metadata carries relative headroom, not an independent white-nits field.
This writer therefore requires exactly 203-nit reference white as its contract.

## Run

Host dependencies: Python 3, NumPy, a C++17 compiler, FFmpeg + ffprobe with
10-bit RGB libx265 support. `make test` also needs a libheif/libde265 decoder.

```sh
make test
python3 verify_file.py build/rgb10_gainmap10.heic
```

The verifier defaults to the available runtime libheif; set
`HEIF_VERIFY_LIBRARY=/absolute/path/libheif.so` to select another decoder.
The build links only the gainmap math into `build/libgainmap_bridge.so`.
No HEIF SDK headers, network downloads, or JPEG libraries are needed.

```python
from heif_save import Frame, save_pair

# sdr and hdr: NumPy FP32/FP64 arrays, shape (height,width,3), linear BT.2020.
frame = Frame(width, height, "capture-123", "warp-final", "beauty-final")
result = save_pair(sdr, hdr, frame, frame, "output.heic", headroom=16.0)
```

`heif_boxes.build_heif()` is an internal trusted-encoder interface, not a
general-purpose validator for arbitrary HEVC inputs. Use `save_pair()` so the
color VUI, actual decoded bit depth, and code-preservation checks run.

## Verified and remaining limits

`QA.json` records reproducible full-file tests. libheif/libde265 independently
reads both coded items from the **saved container**, exposes 10-bit RGB planes,
and returns exact base/map codes. A separate reader validates box references,
metadata rationals and item extents, then reconstructs HDR from decoded items.
The fixture covers all 1024 primary levels and all 1024 gainmap levels. A second
fixture applies a geometry change and local relighting: recomputing the map
greatly reduces reconstruction error compared with the stale map. These are
synthetic math/codec tests, not Natural blush/Purity2 appearance tests.

The installed libheif does not automatically interpret the tmap gainmap. Its
independent decoding proves the two HEVC items and container are readable;
**HDR application is tested by the separate metadata reader**, not by a tmap
viewer. Complete ISO conformance certification, Android Gallery HDR display,
gainmap-aware interoperability, and hardware encoder support are unverified.
The writer declares RExt 4:4:4 honestly; it does not claim Main10 4:2:0 support.
Some consumers may only show the primary SDR image or reject this profile.

This implementation has no Android/JNI integration, Camera2/P010/HLG input,
orientation/EXIF preservation, alpha, multi-frame mode, image grid, AI inference,
style reconstruction, or HDR-base inverse mapping. It is not suitable for
shipping in the app as-is: full-frame copies and lossless CPU HEVC are intended
for host validation, with no phone speed/memory guarantee. Lossless coding can
produce much larger files. Existing missing assets/inference remain separate
blockers for the complete beauty pipeline.

An Android implementation needs an Android-native equivalent of this container
writer and a real 10-bit encoder for **both** items (RGB44410 is not assumed to
exist in MediaCodec), or a verified native libheif gainmap build plus a 10-bit
HEVC encoder. The ordinary installed libheif has no gainmap-writing API and no
encoder plugin here. Upstream libultrahdr's HEIF route currently converts its
base/map to 8-bit; using it unchanged would violate this path's contract. This
directory neither downloads an NDK nor produces an Android `.so` or APK.

## Structure sources

The small writer follows the public libultrahdr metadata serializer and its
libheif gainmap patch, pinned to libultrahdr commit
`66821e0a261aa3a06c0e7c889f52eced52850be1`:

- https://github.com/google/libultrahdr/blob/66821e0a261aa3a06c0e7c889f52eced52850be1/lib/src/gainmapmetadata.cpp
- https://github.com/google/libultrahdr/blob/66821e0a261aa3a06c0e7c889f52eced52850be1/lib/include/ultrahdr/gainmapmetadata.h
- https://github.com/google/libultrahdr/blob/66821e0a261aa3a06c0e7c889f52eced52850be1/cmake/patches/libheif_pr1503.patch
- https://nokiatech.github.io/heif/technical.html (heix includes Main10/RExt)

Only separate rationals are emitted: bit7=3 channels, bit6=base color space,
all lower flag bits zero. `tmap` adds its own leading version byte before the ISO
metadata. There is no JPEG APP2 namespace prefix in this HEIF item.
