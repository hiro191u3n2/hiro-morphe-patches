# Native 2D mesh observation checkpoint

This adds a **private diagnostic observer**, a bounded Android-compatible G1
collector, and an exact-binary evidence verifier. It does not activate the app's
capture processor or constitute a complete face/material binding. No vendor
model, library, shader, authored mesh or original script is distributed here.

## New grounded route

The pinned `libeffect.so` registers `EffectFaceMakeup.templateMesh` as the
component's `+0x90` field. A native per-face update path in `FaceMakeupV2System`
modifies the interleaved CPU positions of that same field and later marks upload
ranges. Thus the name **templateMesh does not imply immutable rest geometry**.
This proves a concrete update path, not that every component takes it on every
frame: disabled/zero-opacity faces and unused slots can remain unchanged.

The scene loop invokes its enabled systems' update virtual slot `+0xa0` and,
when the scene's event condition permits, subsequently emits event `0x44e`.
The Lua dispatcher registers that event for `onLateUpdate`. This proves that
such a callback occurs after that scene's system update loop. It does **not**
prove the camera/algorithm frame identity, completion of every other effect
scene, absence of later GPU deformation, or that the picture callback arrives
after it. The caller must wait for both independently; a missing G1 END cannot
be replaced with the earlier picture callback.

Five selected authored meshes bind their 2-component position data to
**semantic 13, `Amaz.VertexAttribType.TEXCOORD7`**. Purity's eyelash feature
uses semantic 0, `POSITION`, with 3 components. `getVertexArray` selects
semantic 0 and is wrong for the other five meshes. The observer uses the
pinned per-feature attribute; for example:

```lua
comp.templateMesh:getAttributeData(Amaz.VertexAttribType.TEXCOORD7, 0, 8193)
```

The pinned getter returns a `FloatVector`, copies the pinned two or three floats per vertex in the matching
branch, and bounds a positive count to `min(vertexCount - start, count)`.
The observer always uses `start=0`; it rejects the 8,193-vertex sentinel and
accepts at most 8,192 vertices per mesh / 32,768 per feature. No raw native
pointer, guessed ABI, private offset invocation, or read-after-upload cast is
used. It does not change `clearAfterUpload`; absent CPU data causes an ERROR.

## Ownership and protocol

`instrument_geometry.py` first invokes the pinned existing private style
installer, preserving its S1 settings observer, then appends our original
`onLateUpdate` wrapper to the six actual makeup scripts. It only writes a new
caller-owned directory outside the distributable core. Use it in a **new owned
SDK instance for one immutable still**, with the same positive nonce as S1.
Never apply it to an active preview or a subsequent frame. The full original
composer state/resource history and native SDK licensing are still required.

G1 uses separate message ID `0x554c4701`; it is deliberately not accepted as S1
or parsed by `ObservedStyleFrame`. Its per-feature records are BEGIN, MESH,
contiguous POS chunks, and END; any ERROR poisons the whole collector. The Lua
appendix first copies all scalar values into owned Lua tables before sending
any message, so reentrant callbacks cannot alter an SDK vector mid-export.
Repeated late updates emit nothing. The Java collector enforces identity,
nonce, expected feature names, ordinal order, bounds, finite float32 values,
contiguous chunks, complete ends and cross-feature face-count agreement. It
returns owned immutable observations and copies arrays at every public boundary.

`declaredFaceIds` are the component's actual face eligibility list, not a final
active-face/submesh map. `luaUserdataAliasOrdinal` records only equality of Lua
table keys; it is not proof that two wrappers share or do not share a native
pointer. Positions are the complete current mesh attribute snapshot, including
potential inactive/stale slots. They are never split into guessed face blocks.
`productionGeometryVerified()` and `perFaceRoutingVerified()` always return
false. Final matrices, UV/segmentation coordinates, material overrides and
actual draw ordering remain required before these observations can drive HDR
makeup. The retained face count does not authorize interpreting stale slots as
newly detected faces.

## Purity's legacy V2 blusher

Its actual shader receives interpolated `attOpacity`; multiplying only the
slider does not reproduce it. The pinned SWIG table exposes normal
`FaceMakeupV2Feature.getUseAmazing()` and `getAMGScene()`, and the latter can
return null. This is a concrete future read-only investigation route; this
checkpoint never forces `setUseAmazing`, casts the result to an unverified
userdata type, or invents per-vertex opacity. Legacy V2 opacity remains unknown.

The 2026-10-03 follow-up `verify_legacy_accessors.py` checks the complete
null-terminated direct Lua method tables, their class pointers and every name /
wrapper relocation in the pinned library. `FaceMakeupV2Feature` has 24 methods
and `FaceMakeupV2Filter` has 29. Neither table exposes an opacity-array or mesh
readback getter. `setOpacity` and `setIntensityOpacity` are setters; `getUniform`
does not turn the shader's vertex attribute `attOpacity` into a uniform. This
is a bounded negative result for those tables, not a claim that every native
route is impossible. `LEGACY_ACCESSOR_EVIDENCE.json` records the actual method
inventory and exact remaining completion blockers. There is still no
frame-correlated producer for this renderer input, so adding another
caller-supplied opacity interface would not finish the user's requirement.

## Validation

Run `python3 native_geometry_contract/run_checks.py` from any directory, or
pass the two exact supplied archives and JDK explicitly. Validation checks the
pinned native instruction/registration facts, all six real authored position
layouts, actual script syntax, mocked one-shot late-update behavior, defensive
parsing and Lua-to-Java roundtrip, plus SDK36 compilation and D8 min26. Host
fixtures do not establish device rendering, visual equivalence, full-resolution
coordinate calibration or all-lens/all-mode app integration.


## Current native topology diagnostic (D1)

The opt-in `--observe-draw` private instrumenter now adds a second original
observer after the G1 wrapper. It copies the current position attribute,
`TEXCOORD0` UVs, all bounded submesh descriptors and each submesh's current
`indicesCount` prefix from its selected 16- or 32-bit index vector. Unused
backing indices are not mistaken for draw indices. It also reads the material
slot count of the native generated `makeupEntity` renderer; it does not
substitute the authored component entity's renderer when that generated entity
is absent. Native initialization assigns `templateMesh` to this generated
renderer, and a later native path updates its material list. These assignments
are pinned instruction evidence, not a claim about which face occupies a slot.

D1 has its own message ID `0x554c4401` and `NativeDrawObservation.Collector`.
Records are BEGIN, MESH, contiguous VTX chunks, ordered SUB records with
contiguous IDX chunks, and END. All scalar data is copied before transport
callbacks. The parser checks owner identity, nonce, complete sequence, finite
float32 values, UV/position counts, index extents, declared storage counts,
component/submesh order, full ends and cross-feature face-count agreement.
Errors poison the collector. Limits are 8,192 vertices per component, 32,768
vertices and 262,144 exported indices per feature, 64 submeshes per component,
65,536 backing indices per submesh and 24 MiB of total incoming text. Missing
CPU buffers, out-of-range indices and absent generated-renderer components
produce ERROR; the observer never changes `clearAfterUpload` or engine mode.
A missing generated entity is recorded with material count `-1`, not interpreted
as proof that a face is inactive.

`Frame.requireSamePositions(G1Frame)` checks the same owner/nonce and exact
component, face-eligibility and position snapshots across G1/D1. Because G1's
transport happens before D1's snapshot, this rejects an observed intervening
position change. It does not prove that the SDK algorithm result belongs to the
submitted photo or that the final GPU draw uses these unchanged buffers.
UV values are current native values, not enlarged authored UVs. Primitive
codes are retained as observed; non-triangle primitives are not converted.
Material-to-submesh mapping, active face IDs, final matrices, material state and
whole-pipeline order remain unproven. All production-verification flags remain
false, and D1 is not installed in the app candidate.

`LEGACY_OPACITY_EVIDENCE.json` separately pins Purity's supplied legacy shader
and controller. Its output alpha depends on material alpha, intensity and the
interpolated `attOpacity` attribute. The slider controller supplies only one of
those factors. The new Amazing-mesh observer does not expose that legacy
attribute. A read-only native opacity array or a verified same-still diagnostic
render is still required; using the slider as a substitute would be incorrect.

Author validation includes six actual privately instrumented scripts, Lua
reentrant-buffer mutation tests, missing/invalid native field rejection,
32-bit and zero-count index streams, defensive Java parsing, G1/D1 cross-checks,
SDK36 compilation and D8. The existing independent-review report describes its
explicit older G1 source pins until a new review is generated.
