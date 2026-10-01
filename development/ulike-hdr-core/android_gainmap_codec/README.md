# Role-aware Main10 Android codec boundary

This new module connects actual MediaCodec HEVC encoding **and decoding** for the two gainmap HEIF items. It does not relabel the old HLG-only encoder. Every input and decoded output is an owned, full-range, 10-bit P010 frame. The module does not contain RGB conversion or a HEIF writer; `android_gainmap_save` supplies those layers.

| Item role | Android transfer | HEVC VUI transfer | Primaries / matrix / range |
| --- | --- | --- | --- |
| SDR base | `COLOR_TRANSFER_SDR_VIDEO` (3) | BT.709 (1) | BT.2020 (9) / BT.2020 NCL (9) / full |
| Numerical gainmap | `COLOR_TRANSFER_LINEAR` (1) | Linear (8) | BT.2020 (9) / BT.2020 NCL (9) / full |

Android's SDR-video transfer maps to the SMPTE170M internal curve and ISO transfer 1 as its principal mapping. A codec returning transfer 6, although a related curve, fails this exact contract. A numerical gainmap is never advertised as HLG. Metadata alone cannot turn an 8-bit image into 10-bit content.

## Connection contract

`ColorP010.Identity` carries dimensions and capture / geometry / processing revision IDs. `ColorP010` owns three 10-bit code planes and exposes read-only sample buffers. It copies little-endian P010 plane views using the six zero low bits, honors positions / strides / crop offsets, rejects nonzero low bits, and validates all destinations before writing any.

`Main10Codec.encode` selects a codec advertising Main10, P010, and support for the exact dimensions and role. Options select a named codec or prefer / require hardware. Reported hardware status is the manufacturer's report, not a speed or quality guarantee. All encode input is provided through a P010 `Image`; there is no Surface / RGB8 fallback.

An `Encoded` owns immutable bytes, validated SPS/VUI proof, a SHA-256 digest and provenance. Its `verify` import method can validate external bytes but labels their encoder `external-unattested`; the caller's capture identity remains an assertion. `decode` consumes those validated NAL payloads (parameter sets become `csd-0`, Annex-B prefixes are normalized) and returns owned P010 codes and the same original encoded-content digest. It never returns the pre-encode source as a stand-in for the actual decoded base. This allows gainmaps to be calculated against the base actually saved.

The decoder must advertise Main10/P010 and return a P010 `Image` with matching transfer/range/standard, visible crop and timestamp. It receives one validated still access unit, rejects multiple frames, and copies decoded planes before releasing the image and codec buffer. Encoded color, profile, bit depth and visible dimensions are parsed independently from the bytes. The SPS parser reads through VUI and trailing bits; VPS/SPS/PPS references, slice PPS links and SEI structure are also checked. This restricted parser accepts one IDR picture in a single layer, no temporal sublayers, no HRD and no parameter-set extensions. It is not an entropy decoder; the MediaCodec decode step supplies actual decoding.

Worker-thread calls use bounded dequeue waits, deadlines and interruption checks; image and output-buffer ownership is released in `finally` blocks. Image, output-buffer and codec stop/release failures are preserved as suppressed exceptions when an operation already failed. Synchronous platform configure/start/stop calls cannot be guaranteed to obey a hard application deadline.

## Verification

`verify.py` compiles real sources against the Android SDK 36 jar and runs pure Java tests. FFmpeg/libx265 independently generates seven decodable fixtures. Both role-positive 10-bit streams pass; HLG, 8-bit, limited range, transfer alias, wrong matrix, role substitution, malformed SPS tails and missing PPS are rejected. All 1024 P010 code levels, shared UV views, crop positions, readonly sources, bounds-before-mutation and immutable encoded ownership are exercised.

No Android MediaCodec ran in this environment. This is SDK-compiled implementation plus host boundary verification, not device certification, a complete ULike patch, or a claim that the device supports these exact output roles. HEVC VBR and 4:2:0 chroma are lossy; bit-depth preservation does not assert numerical identity with the input. The default 32MP bound is a per-frame limit, not proof that concurrent pipeline allocations fit the phone heap.

```sh
python verify.py --android-jar /path/to/android-36/android.jar \
  --jdk-bin /path/to/jdk/bin --report QA_ANDROID_GAINMAP_CODEC.json
```
