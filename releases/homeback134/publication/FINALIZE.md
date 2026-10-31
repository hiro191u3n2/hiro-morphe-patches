# Final publication evidence

The release inputs below have been completed and `pins.json` is finalized.
Two identical clean builds and three successful final APK applications are bound
in `verification/verified.json`. Piko integration used both source-bundle orders;
Manager sorted actual patch execution identically in both cases. No handset
execution is claimed. The following records the reproducible finalization process;
do not edit older release gates to satisfy this release.

1. Finish source and run two clean builds with `src/build.py`. They must emit the
   exact same bundle, single-patch MPP and JSON QA report. Copy the two QA reports
   to distinct paths under `verification/`.
2. Apply the final aggregate MPP to the exact original Instagram
   452.1.0.53.64 base APK using Manager 2.3.0-dev.7's pinned embedded host. Also
   apply Piko 3.11.0-dev.3 default 64 patches + Hiro (65 total) in both source-bundle orders. The Manager-sorted execution order may be identical.
   Preserve sanitized requests, events and independent audit transcripts under
   `verification/`. Do not put APKs or native APK dumps in the release tree.
3. Verify emitted OneBack runtime against the final MPP for all three outputs.
   For Hiro-only, independently prove every original method body/control-flow
   is preserved except the exact validated insertions. For both Piko orders,
   independently verify the native hooks and emitted runtime.
4. Generate a forward-only Git binary delta named `publication/saved318.patch`
   from immutable317 to the final318 MPP. Both diff paths must be
   `downloads/Hiro_Morphe_Patches_v1.0.318.mpp`; only the forward literal/delta
   stanza is required. Test restoration against the local pinned317 file.
5. Finish `RELEASE_NOTES.txt`, replacing its DRAFT text and stating
   `実機未確認`. Keep the workflow and its publication mirror identical.
6. Refresh main/dev heads and set `pins.json` to the reviewed heads and exact
   final bundle byte count/SHA-256, then set `ready: true`. The publisher accepts
   only one source commit directly after that reviewed main head; both feeds
   must still point to the pinned317 baseline. A branch movement must be
   inspected and reviewed, never silently forced through.
7. Create `verification/verified.json` in this schema. All referenced paths are
   relative to `releases/homeback134/`. `source_and_evidence_sha256` is the exact
   result of `publication/check.py`'s `inventory()` **after** all other inputs,
   notes and pins are finalized. It excludes only this report and generated
   publication artifacts/receipts.

```json
{
  "schema": "homeback134-verified-build-v1",
  "complete": true,
  "physical_android_tested": false,
  "bundle_version": "1.0.318",
  "oneback_version": "1.1.27",
  "artifact": {"bytes": 0, "sha256": "FINAL_SHA256"},
  "original_apk": {
    "package": "com.instagram.android",
    "version": "452.1.0.53.64",
    "bytes": 143237227,
    "sha256": "22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2"
  },
  "builds": ["verification/build-first.json", "verification/build-second.json"],
  "applications": [
    {
      "name": "hiro_only",
      "result": "PASS",
      "physical_android_tested": false,
      "bundle_sha256": "FINAL_SHA256",
      "manager_host_sha256": "4ffa8ccabcc475a18781fcc8642c721eed35ede7d43f967c4f0bd0f51a964a29",
      "input_apk_sha256": "22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2",
      "output_apk_bytes": 0,
      "output_apk_sha256": "ACTUAL_OUTPUT_SHA256",
      "selected_patch_count": 1,
      "events": "verification/hiro-only/events.jsonl",
      "runtime_matches_final_mpp": true,
      "whole_original_native_bodies_preserved": true,
      "audits": [{"path": "verification/hiro-only/audit.txt", "pass_line": "EXACT_PASS_LINE"}]
    }
  ],
  "source_and_evidence_sha256": {}
}
```

The applications array must additionally contain `piko_then_hiro` and
`hiro_then_piko`. These use the same artifact/original/host fields and their own
actual output/event/audit identities. They require:

```json
{
  "piko_version": "3.11.0-dev.3",
  "piko_sha256": "a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3",
  "piko_default_patch_count": 64,
  "selected_patch_count": 65,
  "native_hooks_verified": true
}
```

8. Run `check.py`, `restore_saved.py --base PINNED317 --output TEMP318`, and
   `publish.py --repo REPO --dist EMPTY_DIST --local-only`. Inspect all outputs.
   Publishing is a separate explicit orchestration step performed only after
   these checks succeed. No physical-device success is asserted by this gate.
