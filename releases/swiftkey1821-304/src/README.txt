SwiftKey Beta 1.8.21 / Hiro Morphe Patches 1.0.304

Problem reproduced from published 1.8.20
The previous sensitivity calculation scaled minimum travel before applying an unscaled Android touch-slop floor to minimum upward travel and direction handoff. For a key with shortest dimension80px and touch slop40px, minimumUp is20px at all five levels. At65degrees from vertical, this floor requires47.32px of path length, which exceeds every level's total-distance threshold. All five levels therefore acquire at the same point. The old test grid did not exercise this saturation condition.

Correction
Calculate the exact old Standard values first:
baseMinimum=max(6,min(keyWidth,keyHeight)*0.24)
baseMinimumUp=max(baseMinimum*0.5,jitterSlop*0.5)
baseDirectionSlop=max(baseMinimum*2,jitterSlop*2)
Read the selected multiplier once per DOWN, then multiply all three completed distances. This preserves Standard exactly, scales the jitter-derived floor too and maintains sufficient handoff distance for the existing wide diagonal sector. No additional preferences, native hooks or classes are introduced.
The five saved levels and labels remain unchanged: とても低い2.0, 低い1.5, 標準1.0, 高い0.8, とても高い0.65. Existing selection persists.

Investigation scope
The archived native manifest places settings and KeyboardService in the default process. The native constructor passes Application.getSharedPreferences(defaultName,0) to the existing initializer; both setting callback and recognizer use hiro_up_flick_sensitivity. The compiled runtime retains the setting read and multiplier call. No storage or callback defect was established, so those paths were not modified.
Native longpress timers remain active until custom capture cancels them. Longpress timeout is deliberately separate from flick sensitivity. Edge hit-box behavior also remains native. This release addresses the demonstrated sensitivity-floor collapse, not every possible input cause. No user-device touch trace was available.

Pinned baseline
Single1.8.20 SHA2569d61a9f1c97c339ba77ce3f3fdbc48a2471e8eb5fb0bdbf7736faa103bf53eec
Bundle1.0.303 SHA256a09531a801a03db597cd8243e1a3ae5d7050b652313a1e6d16c0fdfedaf13d30
Extension SHA256b0868d571db1108afef5ab321d9431b308b89de44fe2a3dfac345264715e47e2
Immutable download commit4bc8fd083b51375d200a39258f6ee7122a73f5c4.

Build and preservation
build1821.py supports --download, --input-dir, --tools, --work, --output and --compile-only. Use Java17 for javac/D8 and Java21 via MORPHE_JAVA for the real loader probes. All tool/input hashes are pinned.
Merge1821 replaces the five-class FlickSymbols family while preserving the other76 classes canonically. The final comparison additionally requires that only FlickSymbols$Stroke changes, with zero added or removed classes; all80 other existing classes are unchanged. MPP changes remain limited to META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe. All loaders, dictionaries, resources and unrelated app entries are copied byte-for-byte.
After --compile-only, audit the exact extension with tests/audit_sensitivity_abi1821.py and the archived native APK. The existing AuditSensitivityAbi.java resolves native and Android35 references and callbacks; its evidence is hash-pinned to the exact extension and verifier sources.

Verification
The host suite executes production code and retains the existing settings/gesture tests. It separately compiles the pinned published1.8.20 source to reproduce the collapsed thresholds and show that the regression assertion fails there, then requires the corrected source to distinguish levels. The earlier1.8.19 source fixture continues to verify that default/Standard behavior is unchanged. Tests model platform/native objects; they do not execute Android UI, ART or physical touch input.
Run independent full builds into separate work/output directories and compare all six artifacts. The current QA report distinguishes executed host tests, byte-preservation checks, real Morphe loader probes and local static ABI evidence. Historical files retained in the archive are provenance, not claims that unrelated old suites were rerun.

Publication and limits
publish1821.py requires an independently reviewed source/artifact manifest, exact entry deltas and the regression QA contract. CI verifies source pins, rebuilds twice, matches all artifacts, and publishes release/immutable files before atomically advancing main/dev Manager feeds with expected-head leases. Newer baseline changes abort the publication. Other app metadata and the active ULike policy are preserved.
Handset sensitivity, UI rendering and full clean-original-APK application remain untested. The reference APK is an archived older modified copy; static compatibility is not a handset result.
