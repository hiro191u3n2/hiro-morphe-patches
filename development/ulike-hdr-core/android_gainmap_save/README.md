# Android processed-pair gainmap save

This implements a Java save path for a **caller-supplied, already processed HDR/SDR pair**. It
does not supply the remaining HDR beauty engine. Both images must include the same completed
geometry and appearance edits. Pairing the original HDR with beautified SDR is forbidden: that
would allow a gainmap to reverse the intended edits. Matching IDs are caller assertions, not
proof that the stated edits actually occurred.

The fixed input color contract is nonnegative display-linear BT.2020 RGB, in units of a
203-nit SDR reference white. The SDR is bounded by 1. The caller declares HDR display headroom
greater than 1 and no greater than 10000/203. Scene-linear inverse-HLG samples are **not** this
contract; an explicit scene/display conversion and HDR appearance policy are required upstream.

`GainmapSave.prepare()` performs these operations synchronously on a worker thread:

1. Encode the processed SDR to 10-bit BT.709-transfer codes in BT.2020 primaries, then Main10
   HEVC 4:2:0. Decode those exact coded picture bytes through the selected codec.
2. Generate RGB gainmap bounds and 10-bit codes against that **actually decoded base**, using
   FP64 log2 ratios, explicit 1/64 offsets, and encoding gamma 1. A pair digest rejects changed
   input pixels between the analysis and map-writing passes. A second digest binds the original
   processed SDR/HDR sources from before encoding through final verification, including an SDR
   mutation small enough to fall inside the quality limits. These checks do not make a row
   provider an atomic snapshot; callers must keep both sources immutable throughout the call.
3. Encode the numerical map as Main10 4:2:0, with **linear** transfer metadata. Decode the actual
   map. Reconstruct HDR from the decoded base/map and the final serialized ISO rational metadata.
4. Enforce caller-supplied maximum SDR error, maximum HDR error, and HDR RMSE limits. A failed
   limit rejects the save. Stage a HEIF only after validation. `saveAtomic()` fsyncs a same-folder
   temporary file and atomically replaces the requested destination; it has no non-atomic fallback.

There are no JPEG items or JPEG intermediates. The primary item is a 10-bit SDR `hvc1`; a hidden
10-bit map `hvc1` and an ISO 21496-1 `tmap` item describe the alternate HDR rendition. `dimg`
references are ordered base then map, and the `altr` group records HDR/SDR alternatives.

| Item | Primaries | Transfer | Matrix | Range |
| --- | --- | --- | --- | --- |
| SDR base | BT.2020 (9) | BT.709 (1) | BT.2020 NCL (9) | Full |
| Numerical RGB map | BT.2020 (9) | Linear (8) | BT.2020 NCL (9) | Full |
| Derived HDR | BT.2020 (9) | PQ (16) | RGB identity (0) | Full |

`AndroidGainmapCodec` connects this path to the actual API 33+ `Main10Codec` MediaCodec encoder
and P010 decoder in `../android_gainmap_codec`. The adapter binds decoded pixels to the verified
encoded source and checks frame/role identity. The narrow SPS/VPS/PPS inspector enforces actual
Main10 bit depth and role VUI; an advertised format alone is insufficient. No 8-bit fallback or
HLG label is used to persuade a codec to accept numerical gainmap data.

The RGB/YUV policy is explicit: BT.2020-NCL full-range matrix, 2x2 box chroma downsampling,
nearest-block chroma upsampling, and bounded 10-bit RGB output after inverse matrix. This is
**lossy**; the measured quality gate includes chroma loss, inverse-matrix clipping, and codec
loss. Other viewers can use a different upsampler. The verifier also measures libheif's RGB
conversion. This is not a claim of mathematically lossless full-color maps.

Math and map staging allocate rows, not FP64 full-frame copies. The map uses at most
`6 * pixelCount` temporary disk bytes. P010 codec frames and encoded HEVC remain in memory;
the 32 MP and 64 MiB/item guards are overflow/allocation bounds, **not proof of a phone heap
budget**. Even dimensions are required. No resizing, implicit cropping, or 24.5 MP upscaling
is performed.

Verification:

```sh
python verify.py --android-jar /path/to/android-36/android.jar \
  --jdk-bin /path/to/jdk/bin --report QA_ANDROID_GAINMAP_SAVE.json
```

The suite compiles all production Java against the real SDK 36 JAR. A **host-only** test codec
uses FFmpeg/x265 to produce real Main10 data. The Java pipeline builds four complete HEIF files;
an independent libheif/libde265 decoder reads both coded items, checks actual 10-bit planes,
and verifies final reconstruction using the serialized metadata. Fixtures include black,
smooth/color-edited gradients, and a deliberately changed encoded base to expose stale-map
errors. They are synthetic image tests, not real ULike photos or Android execution.

Remaining limits: no phone execution or codec-support result; no automatic `tmap` display/viewer
test; no upstream completed HDR beauty pair; no app capture/save hook; no EXIF preservation;
no native 24.5 MP input claim. These sources are a validated development component, not an
installable completed ULike modification.
