# Android same-size neural photo component

`BeautyImageEngine` connects the strictly pinned Java model runtime to supplied
full-photo RGB samples. It reconstructs the audited **Natural blush neural pass**
and **Purity2 neural pass**, including their different crop margins,
normalization and blending equations. It does not implement the whole style,
face detection, later makeup/LUT passes, HDR appearance processing or saving.

## Implemented boundary

- Caller supplies immutable `SourceRgbFloat`: width, height, frame identity and
  random-access RGB pixels. Values are **encoded SDR, full range [0,1]**. A float
  container does not make these models HDR-capable; other domains are rejected.
- Caller supplies 106 ordered landmarks for that same still image and its exact
  orientation. The template is streamed from the pinned original library.
  Supplying recent preview landmarks is not valid same-frame correspondence.
  `processWithTransform` additionally supports an explicitly established affine
  matrix. No landmark detector, pose adjustment or orientation is guessed.
- The mask loader accepts only the exact audited ZIP member size/hash, verifies
  PNG structure and CRC, and decodes the original red channel without ICC/gamma
  processing. Vertical orientation is an explicit mandatory argument. No mask or
  template payload is embedded in these sources.
- FP64 inverse warp samples a 256×256 model crop with continuous bilinear/zero
  border. Natural uses the original literal FP32 0.0078 coefficient; Purity uses
  the observed FP32 `(code - 127.5) * (1 / 127.5)` branch. Model inference uses
  FP32 and the strict model-specific ORT wrapper, without injected sessions.
- The generated RGBA crop stays continuous FP64. Projection uses the audited
  replacement's integer pixel centres and coverage `[-0.5,255.5)`. Mask sampling
  uses `(crop + .5) * 1.25 - .5`; Natural opacity is
  `min(mask.r, generated.a) * intensity`, Purity opacity is
  `mask.r * generated.a * intensity`.
- Same-size float rows are emitted. Original float bits, including negative
  zero, are copied unchanged wherever blend weight is zero or outside coverage.
  The component allocates no full-photo array and never creates an 8-bit photo.
  The model itself remains 256×256; this does not upgrade the model to full-photo
  native resolution or create a 24.5 MP camera input.

## API and ownership

```java
PinnedModel.CompiledModel model = PinnedModel.compile(style, modelStream,
        bytennStream, effectStream);
PinnedAssets.FaceTemplate template = PinnedAssets.loadTemplate(effectFile);
PinnedAssets.NeuralMask mask = PinnedAssets.loadMask(styleZip, style,
        explicitlySelectedVerticalFlip);
try (BeautyImageEngine engine = BeautyImageEngine.openAndroid(model)) {
    engine.process(source, sameStillLandmarks106, template, mask, intensity,
        BeautyImageEngine.SDR_DOMAIN, BeautyImageEngine.Budget.standard(), sink);
}
```

The Android model runtime's manifest provider removal and telemetry opt-out
requirements also apply. `openAndroid` sets the process opt-out before ORT loads;
the separately named host verification entry requires that flag already set.

`SourceRgbFloat` must remain immutable during processing, and `frameIdentity()`
must return the same non-null object. Sources and sinks remain caller-owned.
The model engine is synchronized and owns/closes its inference session.

`TransactionalSink.begin` must open private staging storage; each `writeRows`
buffer is borrowed until that call returns. Only the first `rows * width * 3`
values are valid, including the shorter last tile. `commit` must atomically
publish the completed image or throw without publishing it. Every failure after
begin is attempted calls `abort`; an abort failure is suppressed on the original
exception. The component cannot enforce external filesystem atomicity in an
arbitrary caller sink. A public image writer must implement that contract.

## Allocation budget

Dimensions, pixel count, domain, mask style, transform and prospective module
array size are checked before reading source pixels. The default policy permits
at most 25,000,000 pixels, dimension 16,384, eight rows per tile, and an 8 MiB
module Java array budget. The bound includes input tensor, raw model output,
FP64 generated crop, mask, tile, and 16 KiB small-array allowance:

`3*65536*4 + 4*65536*4 + 4*65536*8 + width*tileRows*3*4 + 320*320 + 16384`.

It excludes caller source/sink storage, library/model compilation buffers, ORT
direct/native activation memory, Java object headers and allocator overhead.
Therefore it is **not** a whole-app memory guarantee or evidence that the phone
can capture 24.5 MP. Pinned template loading streams a hash; mask decoding is
bounded by the exact PNG size and 307,520 or 409,920 scanline bytes (RGB or RGBA respectively).

## Verification

`verify_image.py` compiles production sources against Android SDK 36 and ORT
Android 1.30.0, produces DEX at min API 26, and executes separately supplied host
Java ORT 1.30.0. Both actual pinned models, libraries, style masks and templates
are supplied by arguments and never copied into the source directory.

For each style, five 512×384 synthetic fractional RGB fixtures (affine, rotated,
reflected, border crossing and supplied 106-point fit) are independently compared
with the frozen Python full-image component. The comparison allows only final
FP32 rounding plus a 1e-12 FP64 arithmetic allowance. Tests also check raw mask
and template decoding, explicit flipped masks, row-size invariance, exact
zero-weight signed-zero preservation, rollback after begin/write/commit/sample
errors and rejection before source access for invalid dimensions/budget/domain
or degenerate geometry. These are synthetic samples, not facial appearance tests.

No Android device execution, app hook integration, native GPU pixel parity,
complete Natural/Purity style equivalence or HDR preservation is claimed.
