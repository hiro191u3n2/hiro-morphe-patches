SwiftKey Beta v1.8.16 / Hiro Morphe Patches v1.0.299

Purpose
Address transient visual corruption while the expanded candidate grid updates.
Use the published 1.8.15 runtime as the baseline so its left-side candidate
controls and emoji-key toolbar actions remain intact. This release changes only
the CandidateScroll helper family and the MPP version metadata.

Candidate update rendering
The candidate adapter recalculates native row spans when more results arrive.
An existing item's width can therefore change while the default ItemAnimator
still translates that view from its old row position. Reusing the holder with a
non-null change payload prevents one replacement animation, but does not disable
these move animations or RecyclerView's predictive pre-layout.

CandidateScroll now calls setItemAnimator(null) on the RecyclerView owned by the
expanded candidate adapter. The native setter ends current item animations and
removes the animator used for later item transitions. Width and row position can
then take effect in the same native layout without an animated old position.
This policy applies to item updates inside this candidate grid; it does not call
stopScroll, scrollToPosition, fling, or any replacement scrolling implementation.

The existing hooks already reach CandidateScroll at two useful points:
- For non-reset updates, tryAppend applies the policy before checking missing
  snapshots, provider compatibility or geometry. Both an optimized append and
  its native full-refresh fallback receive the policy before their updates.
- snapshot applies it before collecting geometry, after the original full
  refresh and after the native attach refresh. The loader bypasses tryAppend
  for reset=true, so resets perform native span calculation and notification
  first, then disable item animations in snapshot before the queued layout and
  draw. This also covers the initial deferred layout.

The adapter and layout-manager identities must both still match the view before
its animator is changed. A null or replaced view is left alone. The operation is
idempotent when the animator is already absent. No global RecyclerView setting,
theme setting, other list, timer, polling loop or additional native hook is used.

Existing candidate behavior
The previous append and ordering rules remain in place. During additional
expanded results for the same Japanese composing request, preserve the already
displayed sequence and append newly arriving candidates. Matching uses display
text, committed text, prediction input and trailing separator, with separate
queues for duplicates. Every resulting item is taken from the fresh native
result, so selection and long-press metadata stay current. The initial ordering
is unchanged. Changed query conditions, removed candidates and unsupported data
fall back to the incoming native result.

For a compatible append, use native width measurement and row packing, rebind
the existing prefix through a non-null payload and notify the inserted suffix.
This includes fresh metadata for existing visible candidates and changed padding
in the previous final row. Native reset/full-refresh behavior remains available
when the view, provider, theme, dimensions or packed spans are incompatible.
The new animator policy also applies on these full-refresh paths. No stale span
snapshot is reused to hide the visual problem. Geometry caches contain primitive
spans and weak references; candidate matching strings remain local to one call.

Preserved 1.8.15 changes
The original candidate expand and collapse controls remain on the physical left,
with their native listeners and reserved hit areas. The previous standalone
toolbar arrow stays hidden when the dedicated emoji-key route is available.
Normal emoji-key short press opens emoji; long press toggles the toolbar, and a
short press while the toolbar is open closes it. Native timing, cancellation and
post-longpress suppression remain responsible for these gestures.

CandidateToolbar and EmojiToolbar, all working upward-flick code, dictionaries,
Samsung emoji resources, key swaps, input learning, newline behavior, paste and
delete protections, and existing settings remain the baseline bytes. The removed
upward-flick diagnostic preference, collection and copy functions stay removed.
Every other app's patch payload is also preserved.

Preservation and inputs
Inputs are the exact published SwiftKey 1.8.15 and bundle 1.0.298 MPPs at immutable
commit 79e0c7d909aa84f3d6704ec096eab85757d6a4f3. Their SHA256 values are:
  standalone: beccbfb172a84ea198f6ed80c1c498e90cd6b2f18ed653fc1924a70329848993
  bundle:     e0968dee147887d730f5f50775aa78b4c6c5e8fbefb08015bfd47c5aad3aa09a

Tool bytes and the baseline runtime are also pinned. The runtime retains its
71-class inventory with no additions or removals. The built runtime changes only
CandidateScroll.smali; the other 70 classes remain identical in canonical smali,
including CandidateScroll's two nested classes. The builder permits changes only
inside this helper family and rejects any unrelated runtime change.

The exact MPP entry delta is limited to META-INF/MANIFEST.MF and
extensions/swiftkey_japanese.mpe. No archive entries may be added or removed.
All JVM loader classes, including CandidateToolbarPatch and CandidateScrollPatch,
and each package's classes.dex remain byte-identical to the published baseline.
Existing native hooks continue to dispatch to the updated CandidateScroll code.

Build and verification
Use Java 17 for javac/D8 and Java 21 for actual Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1816.py --download \
    --input-dir input --tools tools --work work --output output

The --compile-only mode creates the exact extension for independent native ABI
review. The full build requires tests/native_abi_review1816.json to match that
extension and the pinned archived target APK. Native review checks the actual
item-animator API, the effect of clearing it, and the existing hook paths before
the deferred candidate layout. These are code and API checks, not a handset
rendering test or a complete patch application to an unmodified APK.

The CandidateScroll production class runs in explicit host fixtures. Regressions
cover initial and repeated animator removal, reset and missing-snapshot paths,
ownership guards, current candidate bindings, native span changes, append
fallbacks, query comparison and duplicate-safe result ordering. The old runtime
fails the new initial-animation regression. The report records the exact source
hash, scenario count and assertion count for the version actually built.

Actual old/current Morphe loaders must select the same packaged extension and
preserve all named patches. The workflow validates pinned sources, publication
invariants, two independent complete builds, all six reviewed artifact hashes
and the exact preservation deltas. Only then does the publisher upload, verify
release assets and immutable download bytes, and atomically advance main/dev
Manager feeds using head leases. A changed active baseline stops publication.
Unrelated app metadata and ULike's active policy are preserved.

Scope and installation
Target: Microsoft SwiftKey Beta 9.13.16.4 (1236271168), com.touchtype.swiftkey.beta.
Apply the updated patch from bundle 1.0.299 to the unmodified target and install
using the same signing settings. Source updates alone do not replace the
installed keyboard. No uninstall or keyboard app-data deletion is required.

Host assertions and native code inspection do not establish actual Android
rendering, handset scrolling feel, frame timing, or a complete clean-original-APK
patch application. Those remain unverified. The archived APK used for native
inspection is the same previously modified v2 archive used in earlier reviews.
