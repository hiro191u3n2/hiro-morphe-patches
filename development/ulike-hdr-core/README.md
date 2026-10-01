# ULike HDR processing components — development, not an installable patch

This directory contains new processing components and an audit of the two style
exports supplied on 2026-10-01. It is **not** a completed replacement beauty engine,
an APK/MPP, or a working HDR camera-to-HEIF pipeline. The separate model-export patch is for recording and comparing phone model
files; it does not integrate these processing components into the camera.

## Material acquisition

The fixed exporter ran successfully: both archives use schema
`ulike-style-materials-164`. Every copied material's SHA-256, byte length and ZIP
CRC was verified against the export manifest.

| Style | Copied files | Material bytes | Selected-style files | Named neural dependency |
|---|---:|---:|---:|---|
| Natural blush | 299 | 4,619,450 | 45 | `tt_baoman` inference and face alignment |
| Purity2 | 397 | 5,930,658 | 143 | `idream/tt_goodlike` |

The remaining files include shared beauty resources. The selected style is
resolved by its manifest path, not assumed from a fixed archive directory.
The archives contain shader, Lua, texture, mesh and configuration data. Model
identifiers are present, but those archives do not contain the two main model
files. On 2026-10-01 we subsequently acquired both actual models through the
app's normal metadata/CDN route, without supplied authentication. See
`model_fetch` for exact hashes, sizes and a reproducible downloader, and
`model_inspect` for the inspected containers and decoded graph metadata. The
native ByteNN path for Natural and Espresso wrapper for Purity both lead to the
audited ByteNN core. Both models now run with their actual weights in the host
FP32 implementation in `model_replay`. Its numerical checks compare against an
independent FP64 reference, not full execution of the original Android engine.

The manifests also explicitly do not confirm native execution of every node,
all final parameter values, queue synchronization or a replay round trip. The
360×640 algorithm-branch setting is **not proof** that the saved photograph or
every rendering pass has that resolution.

## Implemented boundaries

* `hdr_input`: a Java P010 input boundary with explicit 10-bit samples, native
  dimensions, color/range and capture identity. It validates strides, buffer
  limits, full-frame crop, allocation cap and matching image/result timestamps.
  It has no NV21/ARGB8888 conversion and is not wired into the existing camera.
* `style_color`: C++ high-precision implementations of the observed LUT lookup
  equations. These operate on an explicitly described, matching SDR reference
  domain. A skin mask is an external input; the model producing it is not
  implemented. Passing HLG/PQ/HDR radiance into an SDR LUT is rejected instead of
  silently clipping the HDR highlights. This is a color component, not the full
  Natural blush or Purity2 style.
* `gainmap`: C++ generation and reconstruction of a gainmap from an already
  processed HDR/SDR pair. It does not reuse the unedited image's gainmap. Encoding
  a HEIF container is now implemented separately in `heif_save`; Android
  MediaCodec/JNI integration and phone decoding remain outside these components.
* `model_fetch`: pinned acquisition of the actual `tt_baoman` and `tt_goodlike`
  model files. Both CDN bodies match the metadata MD5 and recorded SHA-256.
* `model_inspect`: bounded container inspection and exact-hash loaders for both
  acquired graphs and FP32 weight streams. The normal loaders read format data
  from caller-supplied, hash-verified libraries. No model, recovered full graph,
  weight array, vendor library or decoder key is included in the source.
* `model_replay`: independent FP32 execution of Natural's 97 operators and
  239,900 parameters, and Purity's 96 operators and 250,004 parameters. Both
  accept prepared 256×256 three-channel tensors and return four-channel tensors.
  Every layer is compared against a separate FP64 implementation using the raw
  weight layouts. Standard mathematical Tanh replaces the native approximation;
  this is deliberately not a bit-exact copy of the native numerical output.
* `beauty_contract`: the observed per-style normalization, continuous output
  conversion and distinct neural-image blend equations, connected to both real
  models for prepared face crops. These paths contain no uint8 image intermediate.
  Corresponding landmarks and sampled masks are external inputs. This component
  operates in an explicitly declared SDR domain and does not turn an SDR-trained
  model into an HDR beauty engine.
  Its later `fullimage_component.py` adds model-specific 106-point alignment,
  continuous crop sampling and masked projection into an unchanged-size image.
  Both real models and actual supplied masks have passed synthetic full-image
  tests. The model crop remains 256×256; projection into 12.5/24.5 MP host images
  does not establish native camera resolution or model detail at that size.
  Geometry and mask orientation are still explicit caller inputs.
* `android_capture`: standalone API 34+ Camera2 P010/HLG10 source, with explicit
  BT.2020 HLG color, sensor timestamp pairing, physical route validation and
  owned image samples. It compiled against API 36, but has no ULike hook, preview,
  3A convergence controller or actual phone capture verification.
* `android_model_runtime`: Java versions of the pinned model loaders, strict
  graph parser and ONNX generator, with an ORT Android FP32 tensor wrapper.
  SDK 36 compilation and D8 conversion pass. Host Java results match the Python
  reconstruction for both models; independent comparison covers 40,809,984
  intermediate values. Android JNI/device execution and app integration are
  separate remaining steps. The runtime requires startup telemetry opt-out.
* `face_analysis`: exact-hash inspection of all 11 groups in the stock
  `tt_face_v11.1` model, plus an immutable same-still analysis image/result
  boundary and verified rotation/mirror coordinate mapping. It has no callable
  production detector backend. The HDR-to-analysis renderer is now provided by
  `android_hdr_color`; the stock callback probe is separate. The old offline Java callback's
  expected native registrations are absent from the shipped libraries; a
  separate recorder still-to-face callback route has static evidence, with
  initialization, coordinate domain and shutdown still requiring verification.
* `android_hdr_color`: same-size, read-only conversion of the owned P010 frame
  to FP64 scene-linear BT.2020, preserving negative and superwhite values.
  Chroma siting is mandatory caller evidence. A separately declared exposure /
  Reinhard / sRGB rendition supplies continuous float pixels to the SDR model
  and RGB8 only to its analysis proxy. The HDR source remains unchanged; this
  does not create a processed HDR beauty image or reproduce the stock tone mapper.
* `android_beauty_image`: Android Java crop, actual pinned-model inference and
  continuous full-photo composite for both neural passes. Its same-size float
  rows match the independently verified host implementation to final FP32
  rounding. No full-photo float allocation or RGB8 photo intermediate is made
  inside the component. Same-still geometry and mask orientation are explicit
  external contracts. Synthetic P010-to-model integration now passes for both
  models; the output is still an SDR neural component, not a complete HDR style.
* `android_face_backend`: callable stock-SDK single-still diagnostic with an
  owned analysis bitmap, explicit rotation/mirror, initialization-status checks,
  copied raw SDK106 points and worker teardown. It requires normal SDK setup
  and an app-wide exclusive lifecycle lease, neither supplied by this module.
  It is not a production detector or an installed diagnostic UI. Native calls
  and point-coordinate correspondence still require device verification.
* `style_pipeline`: Java double-precision versions of Natural's one and Purity's
  nine named makeup passes, plus the skin/background and final LUT operations.
  The API requires already sampled, same-frame textures, masks, geometry and
  resolved uniforms. Separate shader input images are explicit where the
  original pass does not necessarily read the current destination. Numerical
  shader/asset verification does not establish the original active graph order,
  model-generated segmentation or a complete appearance match. The transactional
  adapter connects real neural float rows to these stages in bounded tiles;
  both real models have passed synthetic makeup-binding integration tests.
  Purity's final
  LUT is JPEG/JFIF despite its `.png` name; no photo JPEG stage is introduced.
* `android_encoder`: Android MediaCodec P010 input and HEVC Main10 selection,
  with named/hardware codec preferences and SPS/VUI validation of actual coded
  bit depth, HLG, color/range and dimensions. SDK 36 compilation, layout guards
  and host HEVC fixtures pass. This is not proof of a phone codec or final HEIF
  output, and the preference is not yet exposed in ULike's settings.
* `android_heif`: Java HEIF mux for a validated, single-frame Main10 HLG HEVC
  bitstream. It writes 10-bit properties, BT.2020/HLG/range metadata, parameter
  sets and clean-aperture crops, with bounded syntax checks and atomic file
  replacement. Crop origins must be even; odd display dimensions are supported.
  Five generated files round-trip through libheif/libde265 with
  exact native 10-bit samples relative to the already coded HEVC. This does not
  undo lossy encoding. It has no gainmap, Android gallery integration, EXIF
  preservation or phone HDR display verification.
* `heif_save`: host writer for a 10-bit RGB SDR base, 10-bit RGB gainmap, ISO
  binary metadata and HEIF `tmap` references. Both coded images round-trip through
  independent libheif/libde265 decoding with 1024 levels. It uses HEVC Range
  Extensions 4:4:4, not a claim of Android Main10 compatibility or automatic
  HDR display. See its QA report for numerical reconstruction error.
* `android_gainmap_codec`: Android MediaCodec Main10 encoder and P010 decoder
  with separately verified SDR-base and linear numerical-map color roles.
  Decoded samples are bound to the exact encoded source. Actual coded SPS/VUI
  and parameter-set linkage are checked, with no 8-bit or HLG-labelled-map
  fallback. SDK compilation and real host HEVC fixtures do not prove a phone
  supports either role.
* `android_gainmap_save`: Java processed-pair save path using Main10 4:2:0 for
  both a 10-bit SDR base and numerical gainmap, then ISO `tmap` HEIF packaging.
  It computes the map from the actually decoded encoded base and validates HDR
  reconstruction with the decoded map and serialized metadata before atomic
  saving. Independent libheif decoding checks real generated files. The
  complete edited HDR/SDR pair is still an external input; this cannot turn an
  untouched HDR original plus beautified SDR into a correct edited HDR image.
* `tools/audit_style_exports.py`: validates source archives without executing
  their scripts. `qa/material_audit.json` records the observed input evidence.

See the component READMEs for exact contracts, unsupported inputs and tests.

## Remaining requirements for the requested complete replacement

1. Complete face detection, landmark/mesh interpretation, skin segmentation,
   native coordinate/orientation correspondence and actual style-stage wiring.
   The sampled makeup/LUT operations now have Java implementations; their
  geometry, segmentation and resolved runtime settings are still external.
   Continuous sampling is implemented for externally supplied geometry, with a
   declared replacement projection convention rather than native GPU parity.
   The relevant bundled dependency
   models exist. Face model group decoding is now implemented separately, but
   its quantized inference and pre/postprocessing, and other feature-specific
   loaders remain unfinished. Confirm the downloaded model hashes
   against the phone's actual cache and compare identical-image reference results.
   The two main model bodies and host tensor inference are implemented; the full
   high-precision beauty engine is not complete.
2. Establish the active execution graph, color interpretation, sampler behavior,
   final style parameters and reference results on identical input images.
3. Implement high-precision processing for those operations and a defined HDR
   extension of effects authored for SDR. The original 8-bit SDK output cannot
   be relabeled as true 10-bit HDR.
4. Integrate the standalone Camera2 P010/HLG source, color conversion, processing, memory
   ownership, lens switching and capture completion with the existing app.
5. Supply the completed processed HDR/SDR pair to the new Java Main10 gainmap
   save path and integrate it with the actual app capture/save lifecycle.
   Verify phone encoding, decoding
   and display, including all requested lens/mode combinations.

The user's current Camera2 diagnostic exposes at most 4080×3060 for the relevant
route. None of this code claims an unscaled 5712×4284 sensor input, removes every
legacy JPEG mode, or proves stock-camera-equivalent quality.

## Verification scope

Host tests exercise the new numerical and data-boundary components. They are not
evidence of a running Android pipeline, successful device capture, accurate
reproduction of the complete styles, or an interoperable final Android HDR
photo. `heif_save` does produce and independently decode real host HEIF files;
existing viewer application of `tmap` and Android display remain unverified. Uploaded
style assets are not redistributed in this source directory.

The staged Java production sources also compile **together** against Android
SDK 36 and the official ORT Android classes, and pass D8 at minimum API 26.
`tools/compile_android_components.py` reproduces this check;
`qa/QA_COMBINED_ANDROID.json` pins the exact source files. This is not an ULike
application build, manifest/JNI packaging test or executable camera pipeline.

The separate [ULike v1.6.5 / integrated bundle v1.0.98 release](https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/ulike-v1.6.5)
adds model observation and export on the phone. Both published MPP files and the
Manager's immutable download were verified against the built artifacts. It does
not install these new inference, beauty or HDR components; see
`qa/PUBLICATION_V165_VERIFIED.json` for the release-specific verification.

The Android input adapter also compiled successfully against the real Android
API 36 SDK in [CI run 36798738616](https://github.com/hiro191u3n2/hiro-morphe-patches/actions/runs/36798738616).
That run verified source commit `b10a4d7c18cfb95505ec5975cec4c55f51727719`.
Its 73 core and 18 adapter-double assertions passed; compiling with the SDK does
not execute a phone camera or an Android framework implementation.
