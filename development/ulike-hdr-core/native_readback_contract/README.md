# Pinned native readback investigation

This is a **read-only evidence audit**, not a callable native bridge. It verifies the supplied original `libeffect.so` and both supplied style archives. The verifier emits hashes, API names and limited instruction facts; it does not copy native binaries, model weights, original shaders or full decompiled routines into this source package. Native addresses are evidence locations only and are never invoked.

## Established type boundary

`BEF::SegMaskInfo.getTexture()` is registered as a one-receiver SWIG call. The wrapper pushes its result through the same registered type descriptor as **BRC::Texture**. That wrapper exposes `addUVFrame`, `getWidth`, and `getHeight`. It does not expose the similarly named AmazingEngine texture APIs.

Separately, AmazingEngine's `RenderTexture.getPixels` is registered with a positions argument. Its native implementation allocates a width×height×4-byte read buffer, indexes pairs of unsigned 16-bit coordinates, reads four byte channels and divides them by 255. This is useful evidence that an engine readback implementation exists. It does **not** establish a safe conversion from the BRC segmentation texture to an AmazingEngine render texture, a callable Lua argument/lifetime contract for the complete path, or calibrated channel/color interpretation. Calling `getPixels` on the BRC object or fabricating a cast/native address would be unsupported.

## Grounded diagnostic route

The selected Natural style's final `AmazingFeature2` at z-order 8003 already samples alpha from the native `share://skinsegmask.texture`. Its exact shader coordinates are `(texcoord1.x, 1−texcoord1.y)`. The selected Purity style's `AmazingFeature8` at z-order 8009 has the same shader and binding; only the color LUT `AmazingFeature9` at 8010 follows it.

A private diagnostic copy can therefore replace the final skin shader with an original small shader that writes the sampled mask to red and known image-coordinate ramps to green/blue. Purity's following LUT is changed to pass its input through. Original mask material, sampler binding, vertices and preceding effect graph remain in that private copy. The existing isolated one-shot `RecordInvoker.renderPicture` callback provides CPU `int[]` output through a normal SDK API; no guessed texture ABI is needed. `android_style_binding` owns this private diagnostic installer; `android_still_analysis` owns the exact-submission, eagerly copied callback image.

This is **sampled diagnostic mask output at callback resolution**, with an 8-bit channel limit. It is not proof of the native mask's original resolution or complete floating-point sample values. Coordinate ramps can detect orientation and transfer mismatches; mismatches must fail calibration rather than be silently corrected with a guessed transform. Actual same-still algorithm timing, texture availability, callback channel interpretation and device execution remain unverified. A successful transport must not automatically mark geometry or full-resolution style reproduction verified.

## Remaining 2D geometry capability

The pinned library registers `Mesh.getVertexArray`; `EffectFaceMakeup` exposes a `makeupEntity` property. Named `onLateUpdate` dispatch appears on a Lua-side path as well as the JS path, and `onEndFrame` dispatch also exists. These are concrete investigation leads. No verified complete route currently proves that a Lua callback runs after the same still's native per-face 2D mesh generation, that a particular returned mesh contains final dynamic positions rather than authored rest geometry, or that it includes the final UVs, segmentation coordinates, per-vertex opacity and explicit face/source routing required by the original draw.

Natural/Purity authored mesh vertex counts cannot safely be filled from the 106-landmark callback by a guessed topology. Purity's older FaceMakeupV2 blusher also generates geometry without an authored mesh file. Until final same-shot geometry, source routing and update order are established, its production binding remains unavailable. Zero-face and multi-face handling must also be explicit; missing geometry is not substituted with zeros or silently omitted faces.

Run `verify_readback.py` with the exact supplied library/archives. `EVIDENCE.json` records verified static facts separately from unestablished runtime capabilities. No phone is connected in the author environment.
