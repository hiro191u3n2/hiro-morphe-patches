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

## Validation

Run `python3 native_geometry_contract/run_checks.py` from any directory, or
pass the two exact supplied archives and JDK explicitly. Validation checks the
pinned native instruction/registration facts, all six real authored position
layouts, actual script syntax, mocked one-shot late-update behavior, defensive
parsing and Lua-to-Java roundtrip, plus SDK36 compilation and D8 min26. Host
fixtures do not establish device rendering, visual equivalence, full-resolution
coordinate calibration or all-lens/all-mode app integration.
