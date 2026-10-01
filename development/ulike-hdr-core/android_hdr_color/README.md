# Owned P010 to scene-light and explicit SDR rendition

`P010SceneSource` reads the same immutable `HdrFrame` produced by `hdr_input`.
It preserves the captured dimensions and 10-bit sample codes and provides
random-access pixels or caller-owned RGB rows in **relative scene-linear
BT.2020**. It never creates a full-photo floating-point buffer. No OOTF, display
peak luminance, exposure adjustment, 8-bit conversion or clipping is applied to
this HDR view. Relative scene light must not be labelled as display nits.

The full/limited range comes from the captured frame's explicit encoding.
Full-range chroma is centred at code **512**, not 511.5. Chroma is reconstructed
by bilinear sampling with edge replication. Horizontal and vertical chroma
positions must each be supplied as `COSITED` or `MIDPOINT`, with a provenance
description. P010 packing itself does not establish these positions. An app
must establish this contract for its capture route; this module supplies no
device-specific default or automatic claim of correct chroma alignment.

The conversion uses BT.2020 non-constant-luminance YCbCr and inverse HLG OETF.
Values above nominal white remain above white. The negative-domain extension
is explicitly **sign-reflected inverse HLG**, a declared processing policy,
not a claim that BT.2100 mandates this extension. Nonfinite inputs/overflow to
the standalone inverse-transfer function are rejected. Signed zero survives.

`SdrRendition` implements both `BeautyImageEngine.SourceRgbFloat` and
`StillFaceAnalysis.ProxyRenderer`. It retains the exact source object token:

1. Apply the caller's explicit relative exposure and luminance-based Reinhard
   compression to scene-linear BT.2020.
2. Convert linear BT.2020 to linear sRGB, explicitly gamut-clip this SDR
   rendition, and apply sRGB encoding.
3. Supply continuous FP32 pixels to the SDR-trained neural component. Only the
   separately requested face-analysis proxy is quantized to RGB8.

This versioned policy is a replacement tone mapper for analysis/model input.
It is **not** the original ULike or Samsung ISP tone mapper, a calibrated
appearance match, HDR model inference, or a processed HDR companion. Keeping
the source HDR unchanged does not mean that a resulting SDR beauty image is
HDR. A final gainmap needs an HDR/SDR pair with corresponding completed edits;
pairing this untouched source with a beautified SDR result would be incorrect.

The component preserves dimensions; it does not create a 24.5 MP camera input.
It depends on `hdr_input`, `face_analysis`, `android_beauty_image` and its model
runtime at compile time. Merely reading color pixels does not initialize ORT.

## Sources and verification

- ITU-R BT.2100-3, Tables 5–9 and HLG equations:
  <https://www.itu.int/dms_pubrec/itu-r/rec/bt/R-REC-BT.2100-3-202502-I!!PDF-E.pdf>
- W3C CSS Color 4 conversion code, rational BT.2020/sRGB primary matrices:
  <https://www.w3.org/TR/css-color-4/#color-conversion-code>

The independent review under `review` checks these equations and boundary
conditions against separate calculations. `../qa/QA_STILL_NEURAL_INTEGRATION.json`
records a synthetic P010 → HDR view → explicit SDR rendition → both actual
pinned neural models → same-size float-row integration test. Synthetic affine
geometry is supplied; no face detection, camera capture, completed HDR beauty,
HEIF saving or Android execution is implied by that test.
