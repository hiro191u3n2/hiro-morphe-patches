# Same submitted still: stock SDK analysis bridge

`StockStillAnalysis.run` is callable from the app, under the same real exclusive
idle SDK lease as `StockStillFaceProbe`. It submits one owned SDR analysis proxy
through the normal, already licensed SDK. It does not use preview landmarks.
The original HDR photograph stays outside this analysis-only RGB8 path.

Implemented:

- The shared native lifecycle now accepts bounded observer hooks. No duplicate
  recorder startup or cleanup is introduced. Original probe callers get no-op
  hooks. Callback exceptions fail the existing ledger and still join workers.
- The face callback eagerly copies 106 points, confidence, visibility, rect,
  track/action/pose fields and any actual `FaceExtInfo` eye, eyebrow, iris and lip
  arrays. No SDK object or native pointer escapes the callback.
- Demographic, emotion and attractiveness attributes are neither copied nor
  retained. The selected original script may use its normal internal attributes;
  instrumentation observes only the resulting makeup uniforms. Declared extra
  landmark counts remain observations; no mesh topology is invented.
- A normal `RecordInvoker.setMessageListenerV2` listener receives bounded script
  exports. The exact stock `libeffect.so` is verified before effect setup; only
  `d40af10b…` is accepted. The existing probe separately checks the exact stock or
  released NV21 `libttvesdk.so` variants.
- Script records are tied to a fresh positive request nonce, one submission, an
  expected feature set and strictly increasing per-feature ordinals. Each
  feature must emit an `END` count. Unknown, duplicate, missing, oversized and
  early matching records fail. Delayed foreign nonces are ignored. Message
  delivery may finish after the face callback; a bounded caller-thread wait
  occurs before normal SDK teardown.
- The output records the digest of the exact owned proxy pixels. Source HDR
  digest, sensor timestamp and settings digest are explicitly caller-declared
  until the capture integration supplies and verifies their relationship.

`EffectSetup.configure` runs after normal initialization on the owned recorder.
The integration must replay actual selected composer paths, modes and settings,
remapping resources to its private instrumented copy. It may not create another
recorder, submit another frame, change SDK authorization, or retain the recorder.
`android_style_binding` owns instrumentation and interpretation of style data;
this transport does not interpret arbitrary payloads as usable geometry.

Protocol: `MessageCenter.sendMessage(0x554c5301, nonce, ordinal, text)`, with text
`S1|feature|kind|face|payload`. Ordinals begin at 1 separately per feature.
`uniform` allows face -1 for a global property or 0..9 for a face; `mvp` and
`vertices` require 0..9. `END` requires face -1 and payload equal to the number
of previous records from that feature. Limits: 8192 ASCII characters per record,
512 records per feature, 4096 total records, 4 MiB total text, 64 features.
The Java snapshot is immutable; script payload validation belongs to style
binding. `ERROR` and other unsupported record kinds fail closed.

Important remaining boundaries:

- No Android phone is connected here. Native startup, Lua message delivery and
  actual face coordinates have not been executed or calibrated. Both
  `productionGeometryVerified` and `declaredHdrSourceBindingVerified` remain
  false even when callbacks are complete. Callback success is not calibration.
- There is no verified CPU skin-mask readback yet. The actual selected styles
  sample alpha from `share://skinsegmask.texture` at `(photoUV.x, 1-photoUV.y)`.
  `getSkinSegInfo`, `SegMaskInfo.getTexture` and texture read APIs exist in native
  registration, but a name alone does not prove texture type, ownership,
  readback signature or alignment. Generic foreground matting is not substituted.
- `enableFaceExtInfo(int)` writes a global SDK flag. This module does not change
  an unknown global value or assume it can restore it. Therefore extra arrays
  are copied only when the SDK actually provides them. A caller may replay a
  captured setting through normal setup; unknown settings remain unknown.
- Native stop/join calls can block independently of Java callback deadlines.
  This module does not claim that a timeout forcibly terminates vendor code.
- No all-lens/all-mode phone validation, HDR beauty integration or production
  release is certified by this module.

Validation: `test.sh` runs 45 host checks of deep ownership, missing/malformed
SDK data, protocol ordering, stale nonces, early/late messages, bounds, timeout
and interruption, then compiles the bridge and shared probe against the real
Android SDK 36. `verify_stock_contract.py` verifies 21 public methods, 15 public
fields and 16 native binding/instruction facts from exact stock files. These
checks establish the callable API contract, not successful device execution.
No vendor binary, model, key, full decompiled class or style asset is included.

An explicit `Request.captureRenderedDiagnostic` opt-in enables one owned raw
packed-pixel render snapshot, tied to the nonce. The probe checks the exact
grid and clones the SDK buffer synchronously; default requests retain no rendered
image. Channel packing, transfer and mask-grid calibration are still unverified.

`PausedStockPreview` additionally implements the concrete normal-SDK
stop-preview, release-native-recorder, run-analysis, restart-preview sequence.
It reads the original backend's actual `g1` surface and checks actual status and
handle at each boundary. `RecorderAdmission` has 25 host state/ownership tests.
This helper is **not enabled or installed in the candidate**: all constructor,
allocation, teardown, application and direct-backend lifecycle paths must first
receive verified global admission hooks. `HOOK_REQUIREMENTS.json` records the
exact initial targets and unresolved bypass coverage. A local monitor or the
startup marker does not establish that coverage. Default-disabled app wrappers
must skip every entry/completion/exception hook consistently; enabling midway
through a process with untracked recorders is forbidden.

The helper also requires the app's actual native-init and composer-restoration
barrier after restart. A successful `startPreviewAsync` result alone does not
prove that the old style was restored. An unknown/failed release or teardown
quarantines admission instead of allowing two native recorders to overlap. No
real phone pause/restoration or all-mode lease safety has been certified.
