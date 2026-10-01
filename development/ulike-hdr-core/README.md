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
`model_inspect` for the inspected container metadata. The native ByteNN call
chain for Natural and Espresso path for Purity have also been identified;
inference has not been replayed independently.

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
* `model_inspect`: bounded container inspection without executing model data.
  Natural blush has a 256×256 face-alignment setting and an embedded ByteNN
  payload. Purity2 also has a 256×256 face-crop setting in its legacy container.
  Inner graph/weight data remain opaque; neither inspector is a running
  inference engine.
* `heif_save`: host writer for a 10-bit RGB SDR base, 10-bit RGB gainmap, ISO
  binary metadata and HEIF `tmap` references. Both coded images round-trip through
  independent libheif/libde265 decoding with 1024 levels. It uses HEVC Range
  Extensions 4:4:4, not a claim of Android Main10 compatibility or automatic
  HDR display. See its QA report for numerical reconstruction error.
* `tools/audit_style_exports.py`: validates source archives without executing
  their scripts. `qa/material_audit.json` records the observed input evidence.

See the component READMEs for exact contracts, unsupported inputs and tests.

## Remaining requirements for the requested complete replacement

1. Complete the tensor/operator interpretation of the acquired models and
   reproduce their inference outputs, face alignment, facial geometry and skin
   segmentation. Confirm the acquired models match the phone's actual cached
   versions. Model acquisition is complete for the two main downloaded files;
   an independent, high-precision beauty engine is not complete.
2. Establish the active execution graph, color interpretation, sampler behavior,
   final style parameters and reference results on identical input images.
3. Implement high-precision processing for those operations and a defined HDR
   extension of effects authored for SDR. The original 8-bit SDK output cannot
   be relabeled as true 10-bit HDR.
4. Integrate Camera2 P010/HLG sessions, color conversion, processing, memory
   ownership, lens switching and capture completion with the existing app.
5. Integrate and validate a JPEG-free 10-bit HEIF writer and gainmap metadata,
   then verify decoding and display on the actual phone.

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

The Android input adapter also compiled successfully against the real Android
API 36 SDK in [CI run 36798738616](https://github.com/hiro191u3n2/hiro-morphe-patches/actions/runs/36798738616).
That run verified source commit `b10a4d7c18cfb95505ec5975cec4c55f51727719`.
Its 73 core and 18 adapter-double assertions passed; compiling with the SDK does
not execute a phone camera or an Android framework implementation.
