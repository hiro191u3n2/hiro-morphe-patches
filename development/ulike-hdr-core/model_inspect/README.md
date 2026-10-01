# Offline model inspection and pinned graph loading

These source-only tools inspect model containers and load the exact graph and weight spans of independently pinned target files. They do not execute models or implement a complete beauty engine. Model binaries and original native libraries are supplied locally by the caller and are not included. The command-line interfaces print bounded summaries, never the complete graph, weights, or loader constants.

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
| `tt_baoman.model` | Two Bach groups. `inference_model.forward_type=4`; type3 `model_name` payload is 970233 bytes and has a BM 00 02 header. The separate pinned BM loader recovers its 97-node graph and 239900 finite FP32 parameters. `face_align` has width/height 256, margin approximately 0.4, offsets `(0,15)`. | Native full-engine execution, face-to-image behavior and device-selected version. |
| `tt_goodlike.model` | Legacy v3 envelope, model-name candidate `tt_goodlike_v1.0`, groups `v0` and `face_point`. The pinned legacy loader recovers `v0`'s 96-node graph, verifies its native checksum and reads 250004 finite FP32 parameters. The separate template loader verifies `face_point` as 106 `(x,y)` pairs, with identical CSV and binary FP32 representations. Plaintext parameters: `CropMarginv0` approximately 0.2, `CropWidthv0=CropHeightv0=256`, `CropOffsetXv0=CropOffsetYv0=0`. | Native full-engine execution, complete face/image behavior and device-selected version. |

The 256×256 values describe face alignment, not the whole saved photograph. The general `model_inspector.py` reports type0/1/2 entries only when their byte representation is canonical; type3 and all unknown/noncanonical entries remain opaque in that general inspector. Its BM recognition checks only magic and the observed size field. Recognized legacy v2/v3 files receive strict outer-envelope parsing based on the native reader at `0xe3dddc`; other legacy candidates receive header-only recognition. The general envelope inspector does not reproduce encoded-payload decoder calls and reports their checksums as unverified. Separate pinned loaders below provide deeper, explicitly bounded results.

`bm_graph_metadata.py` reproduces the normal BM-v2 loader's graph XOR/substitution format using the caller-supplied, SHA-pinned `libbytenn.so`. Both original inputs and the decoded graph are pinned. It validates contiguous segment placement, text terminator, graph/operator count, finite FP32 parameter representation and the shared graph/weight marker. It does not claim to reproduce an unidentified aggregate checksum algorithm. `load_baoman()` returns normalized graph text, unchanged weight bytes and metadata in memory for the separate `model_replay` module. The CLI only prints operator counts and hashes.

```sh
python3 bm_graph_metadata.py /path/to/tt_baoman.model /path/to/libbytenn.so
python3 test_bm_graph_metadata.py
```

`legacy_graph_metadata.py` reproduces the observed Purity `v0` group's ordinary native loader format. It requires the exact caller-supplied model, `libeffect.so` loader and `libbytenn.so` inference library, all SHA-pinned. A bounded reader derives only the loader's immediate data from its pinned instruction sequence in memory; no loader key is embedded or printed. The normal AES-ECB group decoding checks block/plaintext bounds, the pinned decoded graph SHA, the native unsigned checksum and the graph/weight marker. The weight span is separately SHA-pinned and checked for finite FP32 values. The API returns the graph and weights in memory. No license or authentication check is changed or bypassed.

```sh
python3 legacy_graph_metadata.py /path/to/tt_goodlike.model /path/to/libeffect.so /path/to/libbytenn.so
```

This legacy decoder additionally requires Python `cryptography`. It supports only the pinned Purity `v0` group; it does not generalize that group's loader material or operator semantics to face detection, landmarks, skin segmentation, or other dependency files. Those dependency models are present in the original APK, but their individual loader and pre/post-processing contracts are separate work.

`legacy_face_template.py` handles Purity's other group, `face_point`. It verifies the decoded native checksum, separately pinned text and binary hashes, and exact FP32 correspondence between 106 CSV coordinate pairs and the 848-byte binary template. These are alignment points, not an additional neural network. `load_goodlike_face_template()` returns coordinates locally in memory; the CLI emits counts, hashes and crop settings only. Correct use of this template in a full-image face detector/crop/warp pipeline remains a separate contract.

```sh
python3 legacy_face_template.py /path/to/tt_goodlike.model /path/to/libeffect.so
```

`QA_GOODLIKE_GRAPH.json` and `QA_GOODLIKE_FACE_TEMPLATE.json` record these two bounded results. `evidence/SHARED_ENGINE_PROOF.json` traces the Espresso compatibility wrapper to the same ByteNN graph builder and FP32 weight/operator contracts used for the Natural reconstruction; its verifier takes the caller's original SHA-pinned library. `evidence/DEPENDENCY_FORMAT_PROBE.json` records the actual bundled dependency files and the current decoder's inability to validate their feature-specific groups. It does not claim that those models are absent or technically impossible to implement.

`QA_REAL_MODELS.json` binds outer-envelope observations to both input SHA-256 values and contains no raw weights. `QA_BUILTIN_ENVELOPES.json` reports successful outer parsing of 39 additional APK model files. `QA_BAOMAN_GRAPH.json` records the deeper pinned BM result; it does not supersede the general inspector's deliberately narrower contract. `NATIVE_EVIDENCE.json` records the outer layout and pinned library identity. The synthetic test suite covers every prefix truncation of its Bach/v2/v3 fixtures, count/size overflow attempts, name bounds, typed values, opaque payloads, SHA/XOR mismatches, and ZIP CRC/duplicate failures. `INDEPENDENT_REVIEW_BACH.json` records the earlier Bach/header-only review. `INDEPENDENT_REVIEW_LEGACY.json` and `INDEPENDENT_TEST_RESULTS_LEGACY.json` preserve the subsequent independent legacy-envelope review with 473 assertions. `INDEPENDENT_REVIEW_BM.json` records 786 independent assertions on the BM loader, including actual model/library pins and native XOR/substitution instruction checks. Historical review hashes refer to the versions actually reviewed, including the earlier README.

None of these results establishes active-device model identity, style equivalence, HDR input support, or completion of the application pipeline.
