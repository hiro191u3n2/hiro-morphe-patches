# ULike HDR processing components — development, not an installable patch

This directory contains new processing components and an audit of the two style
exports supplied on 2026-10-01. It is **not** a completed replacement beauty engine,
an APK/MPP, or a working HDR camera-to-HEIF pipeline. No Manager release metadata
is changed by this work. The installable baseline remains ULike patch 1.6.4 in
integrated bundle 1.0.97.

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
identifiers are present; their corresponding model weights and inference
implementation are not resolved by these exports. Neither a config file nor an
identifier is a model implementation.

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
  a HEIF container, Android MediaCodec/JNI integration and phone decoding are
  outside the implemented component.
* `tools/audit_style_exports.py`: validates source archives without executing
  their scripts. `qa/material_audit.json` records the observed input evidence.

See the component READMEs for exact contracts, unsupported inputs and tests.

## Remaining requirements for the requested complete replacement

1. Obtain and understand the required neural models and native operators,
   including face alignment, facial geometry, skin segmentation and the style's
   generated face output. Model filenames alone are insufficient.
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
reproduction of the complete styles, or a valid final HDR HEIF file. Uploaded
style assets are not redistributed in this source directory.

The Android input adapter also compiled successfully against the real Android
API 36 SDK in [CI run 36798738616](https://github.com/hiro191u3n2/hiro-morphe-patches/actions/runs/36798738616).
That run verified source commit `b10a4d7c18cfb95505ec5975cec4c55f51727719`.
Its 73 core and 18 adapter-double assertions passed; compiling with the SDK does
not execute a phone camera or an Android framework implementation.
