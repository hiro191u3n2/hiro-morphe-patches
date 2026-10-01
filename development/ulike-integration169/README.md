# Private ULike integration checkpoint 169

This is original development code and a **disabled private candidate**, not a completed camera release. Published ULike v1.6.5 and Hiro v1.0.98 are not changed. `CandidateGate169.enabled()` is permanently false in this build, no `Processor` is installed, and the only added settings UI is a read-only status.

## Concrete application boundaries

- Exact original H0/J0 still requests bind the real camera reader, recorder, selected-style request snapshot, camera session, CaptureRequest, sensor timestamp and physical result. The enabled implementation requests P010/HLG; it never feeds P010 through the old NV21 or Bitmap encoder. The existing OpticalZoom build/capture/burst implementation remains in the call chain.
- The private P010 implementation refuses unsupported burst/reprocess routes. It is not an all-mode implementation. Session/profile/geometry proof and actual device capture remain unverified.
- `SavedUriHandoff169` is an automatic-save-only boundary. It resolves the original callback/controller chain and intercepts the owned dimension callback before the legacy Bitmap-holder worker begins. It calls the actual `AndroidPhotoTransaction.savePair`, inspects its exact owned committed MediaStore item and then posts the original last-photo update, save listeners and shutter cleanup. It does not invent a Bitmap. A committed photograph and a subsequent UI failure are separate outcomes. If the main Handler rejects work, completion reports that UI cleanup could not run.
- The original non-automatic-save Bitmap editor has no new HDR URI route. It is explicitly rejected. This remains an unresolved part of the user's all-mode request.
- The concrete `PausedStockPreview` / `RecorderAdmission` helper is compiled, but its admission hooks are **not installed**. Complete native lifecycle coverage and a real original-style restoration barrier are required before claiming exclusivity.
- Original request snapshots are not native-uniform or GPU-draw proof. Full-resolution same-shot masks, calibrated native 2D geometry and complete style bindings are still missing. The current HDR beauty processor handles one neural face. No placeholder inputs or full application Processor are installed.

## Build and checks

`assemble_candidate.sh` compiles the owned source closure with SDK36, converts the pinned official ONNX Runtime Android Java classes separately, generates exact method contracts, preserves unrelated released runtime/application code and builds two private MPPs. The compile-only OpticalZoom signature stub is excluded from app payloads. Official ORT libraries and license notices are added through the separate reviewed resource helper; its AAR manifest and telemetry initializer are not merged.

`integration/validate.sh` applies private standalone and integrated bundles to the authorized original APKS using Morphe1.16. Exact native/resource checks allow only the existing four-byte NV21 instruction fix, three existing asset contracts and six pinned ORT additions. Static UI binding checks and disabled-branch checks are not substitutes for Android execution.

Inputs required for reproduction are the authorized original ULike5.6.2(740) APK/APKS, the unchanged published v1.6.5/v1.0.98 MPPs, pinned build tools, current `hdr_rebuild167/core` source closure and the reviewed runtime payload generator. Original APKs, decompiled private audit files, user style/model binaries and generated APK/MPP files are excluded from the source allowlist.

No Android phone is connected. Launch, actual new capture, native lifecycle, ORT inference on this phone, hardware codec behavior, HEIF/gain-map viewer behavior, all lenses/modes and finished style equivalence remain unverified. Build/apply success alone does not satisfy the release condition.
