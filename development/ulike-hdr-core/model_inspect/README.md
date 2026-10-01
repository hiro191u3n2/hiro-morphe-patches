# Offline model metadata inspector

This is a source-only investigation tool, not a model executor or a completed beauty engine. It never executes, decrypts, converts, or publishes model weights. It emits bounded metadata and payload byte ranges/hashes. Model binaries are supplied locally by the caller and are not included. Separate local investigations of inner graph formats are not claims made by this inspector.

The Bach packed-buffer layout is derived from the pinned ULike `libeffect.so` parser at `0xc27f94` / `0xc28000`, and checked against the downloaded `tt_baoman` file. The inspector is stricter than the native reader: it checks every count, span, size table, NUL-terminated name and exact end position. It validates XOR8 when the file enables that check. ZIP input is read by exact member name, rejects duplicates/encryption/oversized entries, and validates ZIP CRC. File SHA-256 can be pinned independently.

```sh
python3 model_inspector.py /path/to/tt_baoman.model --sha256 e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0
python3 model_inspector.py /path/to/archive.zip --member exact/path/model
bash test.sh
bash test.sh --models /path/to/local/model_directory --output /path/to/QA.json
```

`--kind bach|legacy|bm` selects an explicitly identified format. Automatic identification is conservative: conflicting Bach/legacy size-field heuristics yield `ambiguous` and exit 2; unknown data also exits 2. Malformed identified input exits 1. Success exits 0, including header-only recognition; callers must inspect the reported status rather than treating exit 0 as successful model decoding.

The current metadata observations are:

| Local file | Verified result | Unverified |
|---|---|---|
| `tt_baoman.model` | Two Bach groups. `inference_model.forward_type=4`; opaque type3 `model_name` payload is 970233 bytes and has a BM 00 02 header. `face_align` has width/height 256, margin approximately 0.4, offsets `(0,15)`. | Tensor graph, weights encoding, actual execution and device-selected version. |
| `tt_goodlike.model` | Legacy v3 envelope, model-name candidate `tt_goodlike_v1.0`, groups `v0` and `face_point`. Plaintext parameters: `CropMarginv0` approximately 0.2, `CropWidthv0=CropHeightv0=256`, `CropOffsetXv0=CropOffsetYv0=0`. | Encoded group payloads, their decoded checksums, tensors, execution and device-selected version. |

The 256×256 values describe face alignment, not the whole saved photograph. Type0/1/2 entries are reported only when their byte representation is canonical; type3 and all unknown/noncanonical entries remain opaque. BM recognition checks only its magic and observed size field. Recognized legacy v2/v3 files receive strict outer-envelope parsing based on the native reader at `0xe3dddc`; other legacy candidates receive header-only recognition. The two encoded payload decoder calls are not reproduced. The native checksum applies to decoded payloads, so this inspector explicitly reports those checksums as unverified.

`QA_REAL_MODELS.json` binds actual observations to both input SHA-256 values and contains no raw weights. `QA_BUILTIN_ENVELOPES.json` reports successful outer parsing of 39 additional APK model files. `NATIVE_EVIDENCE.json` records the layout and pinned library identity. The synthetic test suite covers every prefix truncation of its Bach/v2/v3 fixtures, count/size overflow attempts, name bounds, typed values, opaque payloads, SHA/XOR mismatches, and ZIP CRC/duplicate failures. `INDEPENDENT_REVIEW_BACH.json` records a separate mutation/CLI review of the earlier Bach/header-only revision; it does not cover the subsequently added legacy envelope parser.

None of these results establishes active-device model identity, style equivalence, HDR input support, or completion of the application pipeline.
