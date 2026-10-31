# External profile to Reels — OneBack 1.1.30 / bundle 1.0.321

This release changes Back navigation for browser-created Instagram profiles on
**Instagram 452.1.0.53.64**. It is based on the exact published aggregate318 /
OneBack1.1.27, with the following requested sequence:

1. Open an Instagram profile from a browser link.
2. Press Back on that entry profile: return to the main Reels tab.
3. Press Back again on root Reels: finish Instagram and remove its own tasks
   from Recents using the existing Reels exit behavior.

The first Back retains native profile, child and layer handling. The new helper
acts only after a completed Back has actually removed the registered external
profile, its verified modal parent, or normally finished its owning Activity.
Closing comments or returning from a child video does not itself request Reels.
Predictive-Back cancellation does not run the completion action. The first Back
must not also become the second Back that exits Reels.

## Native route and scope

The existing verified A09 external-profile registration envelope is retained.
This change does not loosen internal-link, URL provenance, username, special
entrypoint or Activity ownership checks. Unregistered and unsupported native
routes retain their original behavior. It does not establish which route was
selected on the reporting handset or prove the original first-versus-second
symptom has been eliminated.

The original Back dispatcher owns profile removal. The existing direct-profile
exit and synthetic profile-handler dispatch are disabled. A native completion
call is placed at each of the three normal returns in the already patched 452
AndroidX committed-Back method. No completion call is added to prediction or
cancellation methods. The independent DEX inverse restores the original
executable body, registers, branches and metadata and rejects altered contracts.

When the Main Activity remains live, the helper uses the verified native CLIPS
selector and reset operations. Selection and wrapper attachment are separate
steps: existing install/draw events wait for the actual destination wrapper and
ready manager before clearing any retained Reels child stack. A subsequent user
interaction or tab choice cancels the pending reset. New external intents revoke
stale navigation attempts. Normal completion of a URL host or a finishing Main
Activity hands the request to Instagram's Main Activity with its native CLIPS
entrance fields and the Piko-compatible startup-tab Intent field. The latter is
limited to this handoff Intent; it does not rewrite the user's stored default
startup tab. This is not a package force-stop or process-kill implementation.

The contract also recognizes the exact Piko navigation/startup method variants:
one conditional replaced by a NOP in `00M2.A04`, and the startup prefix/handler
in `003Q.A07`. Independent inverses compare the remaining original executable
bodies and preserve those Piko mutations. The compatibility proof also recreates
the actual same-pass mutable prefix/catch insertion, serializes it back to the
pinned Piko-only bytes, and rejects eight corrupted variants without relaxing
the general native digest checks. The destination must have an actual
available CLIPS/Reels wrapper; configurations that remove or hide the Reels tab
are not claimed as supported.

The authored patch contains no HB320 diagnostic Toast. The supplied source tree
contains no original Instagram APK, native disassembly or user recording.

## Validation

The new host suite passes **32 cases / 312 assertions**. It models actual native
removal, modern/legacy/direct profile containers, URL-host completion, deferred
wrapper binding, retained Reels children, predictive cancellation, stale intents,
and later user navigation. The retained34 suites also pass with the new requested
profile behavior; host tests are explicit models, not device execution.

The original-APK feature audit passes57 assertions; the independent Piko
compatibility audit passes120 assertions. Build gates preserve282 unrelated
loaders and17 owned loaders, change3 owned definitions and add2 contract
definitions,22 owned loaders total. Runtime gates preserve93 definitions, change5 and add6 helper
definitions,104 total. The number of independently audited native method bodies
remains12; the new completion has3 exact callsites in an already patched method.

Two independent clean builds produced byte-identical aggregate MPP, standalone
MPP and QA reports from192 frozen source files. The aggregate is48,067,555 bytes,
SHA-256 `b466108dcb72831986be9a9292cbbdb3432ebd7a44bf63065740a2ff69646901`.

Manager2.3.0-dev.7's embedded host successfully applied the final aggregate to the
original APK alone (1/1) and with official Piko3.11.0-dev.3 default64 plus Hiro
(65/65) in both requested source orders. Every output passed feature, native Home,
profile-link, manifest, resources and whole-APK audits. Each final APK contains
the exact104 runtime definitions and12 audited native method bodies. The final
profile-to-Reels audit passed86 assertions per output. The two mixed outputs
additionally passed173 assertions each in the independent Piko-preservation audit.
All inputs, source, build proof, MPP, compiled audit tools and output APK identities
were checked unchanged through the final audits.

Piko coverage does not include every optional68-selection combination. Manager's
actual patch execution order was the same for the two requested source orders;
the mixed output APK byte identities differ, and each was independently audited
against the pinned Piko-only baseline. Final evidence is bound into
`verification/verified.json`; the publication gate refuses incomplete evidence.

No physical Android handset execution was available: **実機未確認**. Static APK,
source and host checks do not prove visible transition timing or device behavior.

## Applying the update

Refresh the Hiro source in Morphe Manager and confirm bundle **1.0.321**. Select
「戻る1回で終了・履歴削除」, patch a clean matching Instagram452.1.0.53.64 APK, and
install the generated APK. Updating the source alone does not replace an already
installed patched Instagram. Check both the first and later browser-link entry,
then profile child videos, comments/IME, and Back-gesture cancellation.
