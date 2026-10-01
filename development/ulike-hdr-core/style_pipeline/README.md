# High precision makeup and LUT stages for Android

The two production Java classes implement the remaining **sampled-pixel** makeup
equations and LUT stages declared by the supplied Natural blush and Purity2
style packages. All photo samples and intermediates are double precision. They
accept tiles from the new neural image component without an 8-bit bitmap or JPEG
photo intermediate. This is development source, not an installed ULike patch.

`SampledMakeupPipeline.run` requires one Natural or all nine Purity makeup passes.
`StyleLutPipeline.runPostNeural` then applies the skin/background LUT and, for
Purity, the separate final LUT. It follows the package's outer z-orders. The
caller must explicitly resolve ordering within paired eye renderers at the same
z-order; scene declaration order does not establish actual GPU execution order.

| Style | Declared outer order after neural stage | Shader equation |
| --- | --- | --- |
| Natural | Blusher (2002) | Multiply; premultiplied fragment composed over destination |
| Natural | Skin/background LUT (8003) | Floor-only blue slice; externally supplied skin mask |
| Purity | Lips (2002) | Multiply with segmentation and alpha/intensity/opacity |
| Purity | Blusher (2003) | Multiply using an explicit video-source base, composed over destination |
| Purity | Facial (2004) | Soft light; opacity comes from distance of unpremultiplied red from 0.5 |
| Purity | 3D makeup (2005) | Multiply over explicit `u_basic` source; opaque fragment |
| Purity | Eye pair (2006) | Multiply and screen; external segmentation |
| Purity | Eyelash (2007) | Multiply; external segmentation with different outside behavior |
| Purity | Eye-shadow pair (2008) | Multiply and screen; external segmentation |
| Purity | Skin/background LUT (8009), final LUT (8010) | Floor-only then interpolated blue slices |

Important details preserved by the implementation:

- Lips/eye segmentation coordinates outside [0,1] use weight 1; eyelash uses 0.
  Lips/eye passes discard texture alpha below 0.001; eyelash does not.
- Eyelash's declared `opacity` is unused. The 3D shader has no opacity uniform.
- The 3D opaque fragment still writes its source when intensity is zero. An
  explicit source different from the destination is handled correctly.
- Purity blusher and 3D sources are mandatory caller-supplied arrays. The code
  does not infer that `videoImageTexture` or `share://input.texture` equals the
  current destination framebuffer.
- The facial opacity formula does **not** multiply texture alpha. Covered
  zero-alpha facial samples with nonzero effect strength are rejected because
  the source shader divides by zero there; its actual texture is opaque.
- Both LUT shaders literally use 512 in their atlas addressing, including the
  1024×1024 final LUT. Skin/background uses only the lower blue slice.
- `AmazingFeature9/image/filter.png` in Purity is actually **JPEG/JFIF**, despite
  its extension. Existing authored LUT compression losses cannot be removed by
  changing photo precision. The replacement adds no JPEG photo encode/decode.

## Inputs that are still required

The caller must supply matched, owned still-frame identity and tile coordinates,
face geometry coverage, sampled premultiplied makeup textures, matching
segmentation values and coordinate validity, and final resolved per-face shader
uniforms. The 3D mesh and face/eye/lip geometry are not rasterized by this module.
Purity's V2 blusher also interpolates a per-vertex opacity attribute. Its caller
must supply effective coverage as `geometryCoverage * interpolatedVarOpacity`
and set the scalar `opacity` to 1 (or to a separately resolved global factor).
This is algebraically identical to the fragment's opacity product; a single
uniform opacity alone would not reproduce spatially varying vertex opacity.
See `GEOMETRY_BINDING_NOTES.json` for the pinned vertex/fragment evidence.
Texture samples must have been premultiplied **before** interpolation. The caller
must resolve texture row orientation, upload alpha convention and source routing.
The explicit replacement composition policy for premultiplied blusher/facial
fragments is ONE / ONE_MINUS_SRC_ALPHA; original GPU state was not executed.

The exported `Internal_Makeup` or `Internal_Filter` slider values do not determine
all runtime uniforms. The Lua also consults component opacities, attributes,
application makeup settings and per-face state. Recorded authored defaults and
their source hashes appear in `QA_ACTUAL_ASSETS.json`; these are not presented
as measured final runtime values. No demographic attributes or masks are guessed.

Only explicitly asserted **encoded SDR full-range RGB [0,1]**, with the same
authoring/input encoding, is accepted. HLG/PQ or linear HDR cannot be fed through
these SDR-authored shaders directly. No HDR appearance transform, 10-bit camera
connection, gainmap generation, or complete visual parity is claimed here.

`FrameTile` caps each tile at 262,144 pixels before output allocation. It carries
capture identifier, sensor timestamp, full oriented raster dimensions and tile
bounds. Every pass and LUT mask must match that identity. This prevents accidental
mixing of declared frames; it cannot establish the truth of an identity supplied
by a caller. Arrays are borrowed for synchronous calls and must not be modified
concurrently. Only RGB output is supported; photo alpha is defined to be opaque.
Zero coverage and zero effective weights preserve source bits except the explicit
3D source replacement described above. LUT texels are copied once on construction.

## Verification

```sh
JAVA_HOME=/path/to/jdk bash test.sh \
  /path/ULike_Natural_blush_1790815043265.zip \
  /path/ULike_Purity2_1790815028634.zip
```

The original archives are required locally and verified by SHA-256. No texture,
model, native library, original shader or Lua program is distributed here.
Fixtures decode the source image rows without ICC transformation and explicitly
premultiply straight decoded image samples before linear/clamp sampling. This
fixture convention does not prove the original SDK upload behavior.

Recorded author validation: **220 contract checks** and **43,008 scalar
comparisons from 14 actual-asset fixtures**, maximum absolute error
`2.220446049250313e-16` against independently written scalar shader equations.
Both lip sequence textures are exercised. Fixtures use synthetic RGB photos
with real provided makeup/LUT assets, not claims of visual portrait matching.
See the separate independent review for additional validation.

`compile_android.sh` checks production source against SDK 36 and converts it to
DEX using the caller's existing toolchain. Android execution, actual camera
integration, frame geometry, full-style equivalence and HDR integration remain
unverified/unimplemented dependencies; a successful build is not device proof.
