# Full-size neural-image component with supplied landmarks

`fullimage_component.py` now performs the complete **neural-image component**
for both real models: supplied image and 106 landmarks → model-specific face
alignment → continuous FP64 crop → correct normalization → actual FP32 model
inference → continuous FP64 RGBA → masked projection into the original image.
The output retains the input dimensions. The model's 256×256 face crop remains
256×256; this does not create native 24.5 MP detail or replace the complete style.

The API requires a caller-supplied SDR **RGB** image in [0,1], the correctly
selected/ordered 106 image landmarks, the matching template and an explicitly
oriented neural mask. It does not detect faces, run skin segmentation, or connect
to Android camera capture/saving. It rejects HLG/PQ/linear-HDR domains. The input
RGB transfer/primaries are an explicit caller assertion, not a discovered HDR
training domain.

## New native evidence

The later `CHANNEL_MAPPING_EVIDENCE.json` resolves the prior prepared-channel
placeholder: Natural's RGBA input branch uses colour code 1, drops alpha and
retains channel indices 0,1,2. Its ordinary texture-blit conversion produces
RGBA. A borrowed BGRA buffer must therefore be normalized to RGBA before using
that Natural branch. Purity's valid RGBA/BGRA/BGR/RGB source-format branches all
normalize the model input to RGB. Both native Lua image getters publish the four
output channels as RGBA. This establishes channel order; it does not establish
the active runtime rotation, source transfer function or PNG upload orientation.

The supplied Natural and Purity neural masks are both 320×320. They are separate
from the dynamic skin-segmentation masks used by later LUT passes.
`style_mask.load_neural_mask` reads only the exact SHA-pinned PNG member from the
corresponding supplied ZIP and returns its red channel. `vertical_flip` is a
required explicit boolean: it specifies which PNG row maps to the declared crop
upper row. No original texture-upload orientation is silently assumed, and the
mask binaries are not embedded or distributed in source.

The Purity `face_point` template is byte-identical to the native Natural 106-point
template. Independent native tracing establishes the Purity transform as
`pixel_offset @ margin_padding @ similarity(source_points, template_points)`.
Its margin is float32(0.2) and offsets are (0,0); Natural uses float32(0.4) and
(0,15). `purity_crop_matrix` therefore reuses the already verified pure algebra
with the different **verified** settings. Supplied Natural geometry must already
reflect any required pose/template adjustment. Neither helper performs face
selection or detection.

## Declared high-precision sampler

`WARP_EVIDENCE.json` and `verify_warp_native.py` establish that the original CPU
crop inverts the supplied source→crop affine, samples integer destination pixel
centres without adding 0.5, uses bilinear interpolation with 1/32-quantized
fractions, and uses constant zero for each invalid neighbour. Selected supplied
style files contain no overrides of its default mode/flags/border settings.

The replacement preserves direction, pixel-centre lattice and zero border but
uses **continuous** FP64 bilinear weights. It intentionally removes coordinate
fraction quantization and byte intermediates. Projection uses an explicitly
defined new integer-lattice convention: each original image pixel centre is
mapped into the crop; the covered square is [-0.5,255.5) on each axis; neural
texels use integer centres and the 320×320 mask uses corresponding normalized
edge coordinates. This is not a claim of matching the original GPU's mesh
coverage, half-texel convention or texture origin.

Projection uses bounded row tiles and preserves every pixel outside the covered
crop, or with zero effective blend weight, exactly after FP64 conversion,
including negative zero. It does not resize the full image. Source plus output arrays still
require full-image storage; this NumPy host implementation is not an Android
memory/performance implementation. Input images, coordinates and transforms are
validated, and singular/nonfinite/ill-conditioned transforms fail.

`HostBudget` checks ndarray shapes before scanning pixels or allocating a
full-size output. Its defaults allow 25,000,000 pixels, 600,000,000 bytes for
each prospective FP64 raster/sample output, and 262,144 sampled points per call.
Both 4080×3060 and 5712×4284 RGB rasters fit those shape limits; the latter's
FP64 output alone needs 587,284,992 bytes. This is a host allocation policy, not
a claim that the phone supplies a native 24.5 MP image or that its Java heap can
hold these arrays. Input, output, model and temporary arrays coexist, so total
memory use exceeds the per-output ceiling. A caller can lower or raise the
explicit budget according to available host memory. Row tiles automatically
shrink to the sampling limit; a row that cannot fit is rejected before output
allocation. Use ndarrays for pre-allocation rejection: converting arbitrary
Python array-like objects into arrays remains caller-owned. Oversized broadcast
views and coordinate broadcasts are rejected without scanning their elements.

## APIs

`run_neural_image_component` accepts a verified source→crop affine directly.
`run_landmark_image_component` computes the appropriate style's matrix from
supplied landmarks and template. Both load the exact model through the pinned
per-style loader, retaining the wrong-model rejection of the prepared-face
wrappers. Use the original model/library paths; the caller does not provide a
substitutable ONNX session.

Returned `FullImageResult` contains the full-size RGB image, source→crop matrix
and generated RGBA crop. `native_pixel_parity`, `complete_style` and
`hdr_preserved` remain false. The neural result must still pass through the
remaining native-equivalent makeup, geometry and LUT stages before anyone can
claim complete Natural blush or Purity2 reproduction.

## Validation

With `beauty_contract`, `model_replay` and their dependencies on `PYTHONPATH`:
Start every inference process with `ORT_DISABLE_TELEMETRY=1`, before importing
ONNX Runtime. ORT 1.30's bundled `Privacy.md` documents that its API-only event
opt-out can occur after a startup event. The component requires the environment
setting and additionally disables optional events before creating a session.

```sh
python -m unittest discover -s beauty_contract -p 'test_fullimage.py' -v
python -m unittest discover -s beauty_contract -p 'test_style_mask.py' -v
ORT_DISABLE_TELEMETRY=1 python beauty_contract/verify_fullimage.py /private/tt_baoman.model \
  /private/tt_goodlike.model /private/libeffect.so /private/libbytenn.so \
  /tmp/fullimage_qa.json --natural-style /private/Natural.zip \
  --purity-style /private/Purity.zip --large-raster-checks
```

The real-model verification uses a 512×384 synthetic SDR image, synthetic
106-point geometry, the **actual supplied style mask PNGs**, and both actual
model weights. It explicitly maps PNG row zero to crop upper row for this test;
native upload parity is unverified. Natural preserved 141,528 outside-crop
pixels exactly; Purity preserved 163,288. Both outputs retained 512×384 and FP64
postprocessing precision. Analytic tests independently check sampler weights,
zero borders, transform direction, sub-1/32 precision, model-specific margin,
row-tile invariance, zero-intensity identity and invalid-input rejection.
The optional large-raster checks use both actual generated neural crops and
pinned masks to project into 4080×3060 and 5712×4284 synthetic images. Each check
allocates the full-size FP64 destination and verifies every outside pixel in
bounded row blocks. The source uses a broadcast view to avoid allocating another
full raster. These checks require over 600 MB of spare host RAM and do not
measure phone memory use or establish native high-resolution camera input.

No test uses a real face or establishes visual likeness on the phone. This is a
working model-specific image component with explicit external dependencies,
not a completed camera-to-HDR-save engine. Frozen `README.md` records the earlier
prepared-tensor checkpoint; this document describes the subsequent native
channel/crop findings and full-size component.
