# Android same-still style binding checkpoint

This module replaces several previously caller-invented inputs with observations
and literal caller-owned assets. **It is not a complete Natural blush/Purity2
BindingProvider, and it is not verified on SM-S948Q.** Missing native 2D makeup
geometry, skin/eye segmentation pixels, per-vertex V2 blusher opacity, source
routing and renderer overrides remain blockers. The module never fills these
with guessed landmarks, masks, settings or preview frames.

## Implemented interfaces

- `ShotStyleSettings.freezeAtChoice(state, recorder, shotIdentity, epoch)` reads
  the original selection chain and freezes our existing v164 observer's successful
  composer requests. It preserves ordered raw node descriptors, node tags,
  explicit inline values, successful update values including float bits, nullable
  composer modes/resource path, observer revision and exact object identities.
  `requireCurrent(snapshot)` rejects a revision change or pending work at the
  matching camera request. Pending, overlapping, malformed or dropped histories
  cannot be frozen. It does not establish a native queue barrier.
- `StyleObserverInstaller.install(archive, styleId, appPrivateParent, newChild,
  nonce)` creates a new private selected-effect copy directly on Android. The ZIP
  and each original script must match pinned hashes. The archive is read once
  into an owned bounded snapshot; hashing and extraction consume those same
  bytes, so source-path replacement cannot change verified assets. PNG and
  mesh readers also copy caller arrays before hashing/parsing. Only our own observation
  appendices are in this module. No existing effect directory is changed. A
  fresh owned SDK instance must use each copy for exactly one owned still input;
  the active preview must never load it. The caller retains the private copy
  until native work has safely joined (or retains it with quarantined work),
  then removes only that owned copy; it must not delete assets still in use.
  `Installation.expectedFeatures` and
  `.nonce` feed the still message collector. `instrument_style.py` is the
  equivalent host installer; all 188 output effect files matched byte for byte.
- The appendices wrap the actual selected makeup scripts' `_setOpacity` calls
  and send the final `setFaceUniform("intensity", face, value)` argument.
  They do not infer demographic state or preserve demographic data. Neural
  intensity and LUT alpha are read from the actual material properties.
  Purity's original 3D script exposes its same-update `FaceMeshInfo.vertexes`
  and `mvp`; these are exported in bounded chunks. The 3D intensity readback is
  material-level, not proof that no renderer-level override exists.
- `ObservedStyleFrame.parse(collectorSnapshot, exactFrameIdentity, nonce,
  styleId)` validates completed feature exports, nonce, field grammar, finite
  values, contiguous chunks, complete 1,427-vertex geometry and sixteen matrix
  values. Repeated uniform setters resolve to the last observed value in that
  update. An absent setting remains absent. The passed identity must be the
  analysis outcome's owned still identity; a random caller token is not proof of
  source provenance. The script has no independent sensor timestamp API.
- `AuthoredMesh.readPinned(bytes, sha256)` reads literal UVs and triangle lists
  from the caller's audited serialized meshes. It deliberately never exposes
  authored rest positions as live face geometry. There is no 106-to-248-point
  mapping guess. Triangle-strip quad files are rejected; they are not needed by
  this makeup mesh path.
- `PinnedPngTexture.readPinned(bytes, sha256)` decodes actual 8-bit RGB, RGBA and
  palette PNG samples without ICC/gamma conversion. Literal authored texture
  precision is preserved; photo pixels are not quantized. The explicit sampler
  premultiplies before bilinear filtering when requested, uses clamp-to-edge,
  and requires a declared row orientation. Original GPU upload/sampler parity
  still requires a device check. The mislabeled Purity final JPEG LUT is rejected
  by this PNG decoder; its existing loss cannot be undone.
- `Purity3dBinding.resolve(...)` binds one observed 3D face to the exact pinned
  1,427-vertex/2,304-triangle UV/topology and 512×512 material. It perspective-
  interpolates both original UV sets, including the authored makeup V offset
  `+0.085`. The background UV follows the shader's interpolated `uv1`; it is not
  assumed to equal the output pixel coordinate. An explicit same-frame
  `ShaderBase` samples `u_basic`. The result is the existing pipeline's
  `ResolvedPass(PURITY_3D, ...)`, suitable for the corresponding entry in a
  future complete `BindingProvider`.

The 3D rasterizer has an explicit replacement contract: OpenGL clip space to
an image with top row zero, no culling, no depth buffer, last triangle wins,
one pixel-center sample. Near/far-plane clipping is unsupported and rejected.
This contract does not reproduce an unobserved original GPU state, MSAA, or
multi-face draw order. It is a tested material binding building block, not an
assertion of final visual quality or full-style equivalence.

## Bounded skin-mask diagnostic route

`SkinMaskProbeInstaller.install(...)` creates a separate private one-still copy
and validates the exact original skin fragment, vertex shader and material
hashes before changing only the final skin fragment. It retains the real
`maskTexture` binding and samples its original alpha at `(u, 1-v)`. The diagnostic
output is `R=sampled mask, G=u, B=v, A=1`. For Purity only, the later final LUT
fragment becomes a plain pass-through so it cannot recolor that diagnostic.
The original photo, earlier scripts/materials/geometry and high precision
photo processing buffers are not replaced by this output. The result must
never be saved as the user's photograph.

The SDK backend's explicitly enabled `renderedDiagnostic` callback supplies its
owned raw packed `int[]`, dimensions and request nonce. `DiagnosticSkinMask`
requires an explicitly declared byte-channel interpretation, row origin and
horizontal mirror. It checks alpha and the entire G/B pixel-center UV grid
within one 8-bit code. A transfer, rotation, scaling or packing mismatch rejects;
there is no inferred gamma repair. It retains only R and returns same-frame
mask tiles in the declared top-left raster. This checks transport interpretation;
it cannot prove R came from the expected native mask or settle global graph
ordering. Source identity, callback ownership, actual device channel packing
and a real mask-versus-reference check remain mandatory.

The owned SDK analysis backend caps its proxy/diagnostic grid at 4,194,304
pixels. This parser preserves that grid and rejects tiles with different
full-image dimensions. It performs no enlargement or implicit proxy-to-native
geometry mapping; native 4080×3060 alignment is therefore still unverified.

The callback quantizes sampled GPU mask values to 8 bits. Even an underlying
8-bit segmentation texture may produce fractional values after interpolation;
`exactNativeMask=false` and `originalMaskPrecisionPreserved=false` are deliberate.
This is an implemented, host-tested diagnostic extraction path, not a claim
that a real-device mask has already been obtained, that native precision is
preserved, or that the full style is complete. Skin-mask texture userdata is
`BRC::Texture`, not `Amaz.RenderTexture`; no guessed `getPixels` cast is used.

## Same-still integration boundary

The app bridge can freeze settings at `StyleStill4.choose(state, recorder)` and
match the original `VERecorder.b` against the low-level capture callback's
`TECameraVideoRecorder` outer object before accepting the camera request. The
separate `android_still_analysis` module owns the exact still submission and
normal `RecordInvoker.setMessageListenerV2` transport. Its `EffectSetup` callback
runs on the new owned SDK instance before the one still submission, with the
listener installed first. Install this module's private effect copy using normal
composer methods there; never load it in a preview recorder.

Grounded SDK methods include `setComposerMode(int,int)`,
`setComposerResourcePath(String)`, `setComposerNodes(String[],int)`, and
`updateComposerNode(String,String,float)`. Their presence does not justify
inventing a default setup sequence. All required original resource roots,
mode, graph ordering, global builtin state and updates must be reconstructed
from actual frozen requests and validated at runtime. Both supplied v164
exports explicitly mark `ordered_api_model_complete=false`. Their selected
material path also differs from the shared resource receiving makeup/filter
updates (`2000_5_d`). A selected-style directory alone does not reproduce the
whole original composer graph. This module does not silently replace unknown
setup with `setNodes([selectedStyle])` or hardcoded intensity.

The public transport uses message ID `0x554c5301`, one positive nonce, per-feature
ordinals starting at one, and `S1|feature|kind|face|payload`. `uniform` can use
face `-1` for a global material; `mvp` and `vertices` use actual face indices.
`END|-1|count` is emitted even when no face is present. The collector rejects
unsupported ERROR records, missing END, unexpected features, duplicates,
incorrect order or budget overflow. Each payload is at most 7,000 characters,
within the collector's 8,192-character record limit. Emission is post-setter /
post-original-onUpdate observation; it is not a GL readback or draw-completion
fence. Device delivery and one-still attribution remain to be tested.

## Asset findings and remaining requirements

| Material | Audited authored mesh | Runtime information still required |
|---|---|---|
| Natural blusher | 1,240 vertices; 5 submeshes × 371 triangles | Native deformed 2D positions, active face/submesh, color/opacity overrides |
| Purity lips/facial | 1,240 vertices; 5 submeshes × 371 triangles | Native 2D positions, lip/skin masks and animation frame where used |
| Purity eyes/shadow | 870 vertices; 5 submeshes × 334 triangles | Native 2D positions, segmentation coordinates and same-z draw order |
| Purity eyelash | 1,044 vertices; 6 submeshes × 334 triangles | Native 2D positions and segmentation pixels |
| Purity 3D | 1,427 vertices; 2,304 triangles | Device-confirmed exported geometry, draw/source/sampler state and overrides |
| Purity V2 blusher | Native generated geometry; texcoord text asset | Actual geometry plus interpolated `attOpacity`; not a global slider |
| Natural/Purity skin LUT | `maskTexture = share://skinsegmask.texture` | Actual same-still alpha pixels and texture orientation, not a fake face oval |

The original skin LUT samples mask alpha with vertically flipped photo UV.
The normal still SDK's 106-point and extra-landmark arrays do not establish the
missing native makeup topology. Merely finding `getVertexArray`, `getSkinSegInfo`
or `getPixels` strings does not establish when the geometry is updated or a
working CPU texture-readback signature. Further implementation must use verified
normal SDK access or a proven same-still diagnostic render pass.

## Reproducible validation

Run `python core/android_style_binding/run_checks.py` from the existing work tree
(or pass the input paths explicitly). It uses the pinned local SDK36/JDK and
caller-owned style archives; it never writes copied vendor assets into this
module. `QA.json` pins production and dependency source hashes.

- 13,485 request-freeze, mesh and message/parser contract checks.
- 113,181 texture and geometry checks. Seventeen real PNG decodes match
  independent Pillow RGBA digests; premultiplied and straight bilinear sampling
  are checked separately.
- Real Purity 3D UV/topology/texture with **synthetic planar live positions**
  covers 6,792 pixels in the analytic raster test. This is not a device face test.
- Twelve supplied original scripts load under Lua 5.4; fifty update/transport
  cases use explicit SDK mocks. The original makeup setters are exercised;
  Purity neural observer-only behavior uses a mock update because the original
  update requires native GAN geometry.
- 10,744 diagnostic mask interpretation/identity and private shader-install
  checks, using synthetic callback grids; no actual device calibration.
- Nineteen Android installer guards; 188 copied/patched effect files exactly
  match the Python installer. SDK36 compilation passes for all production Java.

No native device execution, actual render synchronization, complete style graph,
HDR visual equivalence, gain-map validity or full app integration is claimed
by these host checks.
