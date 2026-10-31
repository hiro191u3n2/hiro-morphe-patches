# Release135 publication evidence

Target: OneBack **1.1.28**, aggregate **1.0.319**. Final evidence is complete
and `pins.json` now binds the exact artifact and35 host suites. Two independent
clean builds matched. Three final APK applications and all six audits for each
output passed. This document records the reproducible finalization process;
physical Android execution is not claimed.

The baseline is the exact published **1.0.318** MPP, 48,032,024 bytes,
SHA-256 `170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af`,
at immutable distribution commit
`9cb5e46a2df3d9eb3a4ad8700f0be22b4a694ecd`.

The current report distinguishes the initial external-profile launch from
subsequent launches of the same link: on 318 the first Back returns to Home and
the next Back exits, whereas a later opening of the link can exit directly.
This is user evidence about **318**, not handset validation of the future319
implementation. The source, tests and final release notes must describe the
eventual proven correction without claiming the new version has run on a phone.

## Required final inputs

1. Finish source, including a regression for the initial-launch difference and
   preservation of nested videos, dialogs, IME and cancelled Back gestures.
   Run all 34 retained suites plus the new regression suite(s). Set
   `pins.json.host_suites.count` to the exact final count greater than34 and
   `host_suites.new_required` to their actual `Back135...Test` class names.
   The final count is35, including `Back135InstagramInitialProfileTest`. The final artifact and verification inputs are now fixed.
2. Produce two independent clean builds from `src/build.py`. Both MPPs and the
   JSON QA reports must match exactly. Expected report schema is
   `homeback135-build-v1`; metadata audits must emit `PASS Metadata135Audit`.
   Copy the two reports to distinct `verification/` paths.
3. Apply the final319 MPP to the exact original Instagram452.1.0.53.64 APK using
   Manager2.3.0-dev.7's pinned embedded host. Also apply Piko3.11.0-dev.3's
   default64 patches plus Hiro in both requested source-bundle orders. Record
   actual execution order rather than implying the Manager necessarily runs
   them in the supplied order. Preserve final event logs and independent audits.
4. Verify emitted runtime matches the finalMPP for all three outputs and that
   original code, manifest and resources remain preserved outside the scoped
   hooks. Piko outputs must be compared against their exact Piko-only baseline.
   No APK, native disassembly, user video or extracted app resources belong in
   this public source tree.
5. Generate `publication/saved319.patch`, a forward-only Git binary delta from
   pinned318 to the exact final319 artifact. Its target path must be
   `downloads/Hiro_Morphe_Patches_v1.0.319.mpp`. Test the restored byte count and
   SHA-256. Do not rebuild or recompress an artifact during publication.
6. Finalize `RELEASE_NOTES.txt`, remove all DRAFT wording, and retain the explicit
   `実機未確認` limitation. Refresh reviewed main/dev heads; do not silently
   bypass a changed branch or active feed. Fill the exact artifact identity and
   new host expectations before setting `pins.json.ready` to true.
7. Create `verification/verified.json` using schema
   `homeback135-verified-build-v1`. Its structure is the same fresh-evidence
   structure enforced by `check.py`: complete/version/artifact fields, original
   APK identity, two build-report paths, and three application rows named
   `hiro_only`, `piko_then_hiro`, `hiro_then_piko` with their actual identities,
   successful events and audit markers. All referenced paths are relative to
   `releases/homeback135/`. `physical_android_tested` remains false.
8. Set `source_and_evidence_sha256` to `check.py.inventory()` after all source,
   evidence, notes, workflow and pins are final. The inventory excludes only the
   final report itself and generated publication artifacts/receipts. Keep
   `.github/workflows/homeback135-publish.yml` identical to `workflow.yml`.
9. Run `check.py`, `restore_saved.py --base PINNED318`, and
   `publish.py --repo REPO --dist EMPTY_DIST --local-only`. Inspect every result.
   Only then orchestrate the reviewed source commit/publication. The publisher
   retains exact-parent and branch leases, atomic main/dev Manager updates,
   immutable download verification and public release-asset byte checks.

Do not alter earlier releases or weaken their verification gates. Drafting
these files does not authorize an unverified publication.


## Final implementation scope

The canonical release is prepared in `repo135`. Only the exact native-created,
registered external profile directly attached to MainActivity's root container
receives the additional recognition path. The Home/tab selector may be null or
point to a retained tab; all input/layer/popup/child/transaction/gesture guards
remain. The only changed runtime definition is `InstagramExternalProfile`;
97 others remain byte-identical to318, for98 total definitions. The new loader
class is `InstagramInitialProfileContract`; there are21 owned loader definitions
and282 unrelated definitions retained.

Final evidence must use `build135-clean-first` and its independent clean second
build, and their final APK applications. Earlier experimental modal-route
changes, prototype fixtures and prototype test logs are excluded. A positive
result from those experiments is not evidence for this final implementation.
The new initial-profile suite has38 cases and276 assertions; the318 negative
control has17 failures and21 retained cases passing. Validate these summaries
against the final logs when freezing the report.
