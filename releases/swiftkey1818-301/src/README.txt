SwiftKey Beta v1.8.18 / Hiro Morphe Patches v1.0.301

Purpose
Keep the expanded candidate list at the viewed position when results for the
same composing request are reordered or removed. Earlier releases optimize
compatible appends and disable item animations; a different native refresh path
can still reset the candidate panel to position zero. This release addresses
that refresh path while preserving the published 1.8.17 punctuation changes.

Candidate refresh and scroll position
The same-query update must cover both the candidate adapter and the expanded
panel's reset listener. The adapter still receives fresh candidate data and
native span calculation. The panel must not reset the viewport or stop its
ongoing scroll merely because the same query produced a different result list.
The update is scoped to the candidate provider's notification transaction so
listener order cannot leave one of the native reset paths unprotected.

A layout that changes the visible rows can emit onScrolled(0,0). The native
result listener checks its visible-position watermark even for this event, so
clearing that watermark during a refresh can immediately request another page.
For the zero-delta layout callback owned by the same candidate update, rebase the
watermark to prevent that extra fetch. The original listener body and additional
result loading triggered by actual scrolling remain in use.

The runtime checks that the results belong to the same Japanese composing
request and that the existing expanded candidate view can participate safely.
Changed input and unsupported native state retain the normal refresh behavior.
Existing append handling and candidate ordering rules remain available where
applicable. Current candidate objects continue to supply committed text and
long-press actions; native measurements determine the current row widths.

The candidate-only ItemAnimator suppression from 1.8.16 remains active, including
native full-refresh paths. This release does not introduce a global animation
setting or an alternative gesture recognizer. Host checks cover position
preservation, both native listener orders, fresh selection metadata and the
same-query reset path without stopScroll. They also cover avoiding an extra
fetch from the update's layout-only callback. Actual handset behavior is
unverified.

Preserved 1.8.17 behavior
Japanese QWERTY ordinary tap on 。 inputs 。, while its upward flick and alternate
action select only full-width ？. Ordinary tap on ー inputs ー, while its alternate
selects only full-width ！. Both secondary key labels still match their alternate
characters. The punctuation resource normalizer, native factories and original
normal-tap actors remain unchanged, as does the working upward-flick recognizer.

Candidate expand and collapse controls remain on the physical left. A normal
emoji-key short press opens emoji; long press toggles the toolbar, and a short
press while the toolbar is open closes it. Existing key swaps, input learning,
dictionaries, Samsung emoji, newline behavior, paste/delete protections and
settings remain intact. The upward-flick diagnostic preference, collection and
copy functions stay removed. Other apps in the combined MPP are preserved.

Runtime and loader scope
Only the CandidateScroll runtime family may change. Every unrelated runtime
class is compared against the exact published 1.8.17 baseline in canonical
smali. The report records the actual changed, added and removed class inventory.
This includes preservation of KeySwap, PunctuationKeys, CandidateToolbar,
EmojiToolbar and FlickSymbols code.

The existing CandidateScrollPatch helper is updated in both loader formats.
The MPP delta is exactly META-INF/MANIFEST.MF, extensions/swiftkey_japanese.mpe,
classes.dex and app/hiro/swiftkey/patches/CandidateScrollPatch.class. No archive
entry may be added or removed. The other ten existing SwiftKey JVM patch classes,
including the main loader and toolbar/punctuation helpers, stay byte-identical.
The standalone and combined MPPs have their own Android loader DEX; their updated
CandidateScroll helper and selected SwiftKey runtime must agree. Dictionary
resources and every other app payload remain byte-identical.

Inputs and reproducibility
The baseline is the exact published SwiftKey 1.8.17 and Hiro bundle 1.0.300 at
immutable commit 577492df2f5841c826f3710df535b39bbde1f55a.
  standalone SHA256: d67006d5b338765a256518e6e2a2363d74de30afa6e593bdd0eb9a740f4b5f14
  bundle SHA256:     f3fd6bdec26620497a13fb840419c02713357f80f624ac902f3063acb70ac69a
Both inputs, build tools and all reviewed source files are pinned. The build
rejects an unexpected archive change or a change outside the runtime family.

Verification
Production candidate code runs in explicit host fixtures for the provider
notification and panel-reset interaction. Reports include the exact production
source hash, scenario count and assertion count. No final count is inferred from
older releases. Tests of unchanged punctuation behavior from prior releases do
not become a claim of new handset testing.

The native candidate anchor review checks the relevant original APK methods,
notification path and reset effects. The strict ABI review in
 tests/native_abi_review1818.json
must match the exact merged extension and pinned archived target APK. Native
hook probes cover the saved method fixture and archived target under both the
old and current Morphe APIs. Actual Morphe loaders must select the packaged
extension, preserve named patch registrations and retain dependency structure.

Build with Java 17 for compilation/D8 and Java 21 for the Morphe loader:
  MORPHE_JAVA=/path/to/java21/bin/java python build1818.py --download \
    --input-dir input --tools tools --work work --output output

The --compile-only mode prepares the exact extension for independent ABI review.
The publication workflow verifies source pins and offline publisher invariants,
then performs two independent complete builds. Both outputs must match all six
reviewed artifact hashes and the preservation contract before publication.

The publisher verifies uploaded release bytes and immutable MPP downloads before
advancing main/dev Manager feeds atomically with expected-head leases. It then
checks the committed metadata, unchanged ULike policy and public branch content.
Public raw URLs can lag immediately after a commit; only that freshness check
has a bounded retry, with a fresh query and both head leases checked on every
attempt. Feed and changelog bytes must still match exactly. Persistent mismatch
or concurrent changes stop completion. The publication receipt is saved to both
branches and uploaded only after these checks pass.

Scope and installation
Target: com.touchtype.swiftkey.beta, 9.13.16.4 (1236271168).
Update the Manager source to 1.0.301 and apply the SwiftKey patch to the unmodified
target APK. Install using the same signing settings. A source update alone does
not replace the installed keyboard. Existing app data and settings can remain;
no uninstall or data clearing is required.

Host checks and static native inspection do not establish handset scrolling
feel, frame timing or a complete clean-original-APK patch application. These
remain unverified. The archived APK used for native inspection is a previously
modified archive; it is not evidence of an original-APK apply test.
