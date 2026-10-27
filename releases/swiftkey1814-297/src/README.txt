SwiftKey Beta v1.8.14 / Hiro Morphe Patches v1.0.297

Purpose
Improve the expanded Japanese word-candidate list's append/update path, and
remove the upward-flick diagnostic feature completely. The user confirmed
1.8.13's upward flick works well; preserve its input behavior and thresholds.

Candidate order during additional loading
The existing emoji supplement places its first match near the front and the
remaining matches after the native word candidates. When another native page
arrives, those new words appear before the already displayed emoji tail. Native
rb0.j compares the old and new prefixes, classifies that interleaving as a reset,
and causes a full grid refresh. This explains why handling only reset=false
would miss the supplementary-candidate path seen in the user's screenshot.

The new stabilize(rb0.j, ib0.a) bridge runs before that native comparison. Only
an EXPANDED result with more candidates and a matching Japanese composing query
is eligible. The query guard mirrors native l40.c.equals except its growing
result-limit field g; it includes the preceding Sequence, TouchHistory,
capitalization, verbatim/filter/language modes and composing strings. Match each
previously displayed candidate to a fresh incoming candidate by display text,
commit text, prediction input and trailing separator.
Keep that previously displayed sequence, then append the unmatched new entries
in their incoming order. Duplicate candidates use separate queues of incoming
positions, so they are not silently merged or dropped. Initial results,
non-expanded results, unchanged/shrinking lists, context changes, missing old
candidates or unsupported data retain the incoming native order.

The stabilized list always uses the fresh incoming candidate objects. It also
retains the exact incoming breadcrumb, result kind and request object. No old
commit/long-press metadata is reused, and no candidates or candidate text are
stored in the geometry cache or written as diagnostic data. The initial ranking
is unchanged; during additional loading, preserving displayed positions takes
precedence over inserting new words ahead of already visible supplemented items.

Candidate append updates
The archived target's expanded-candidate adapter also refreshes the entire list
for a true append, invalidating already visible rows during scrolling. A narrowly
scoped append hook uses range updates when the provider, layout, dimensions and
existing span layout match a recorded snapshot. It rebinds existing candidates
with a non-null payload, then notifies the appended range separately. This keeps
existing holders reusable instead of invalidating the whole dataset. The native
binder remains responsible for refreshed commit and long-press metadata; its
binding/reset flag is not changed.

Use native width measurement and row packing. Verify that spans before the old
final row remain unchanged, allowing the final row to lose its padding as new
words fill it. Reset/replacement or unsupported layouts retain native full
refresh. Candidate selection and additional-result retrieval continue through
the existing native implementations.

These changes address the observed code paths that reorder the supplemented
tail and invalidate the candidate grid. They are not a measured frame-time
improvement or a claim that every source of stutter has been identified on the
user's device. No speed figure, Android-device test or full original-APK apply
test is asserted.

Complete diagnostic removal
Remove the settings item, clipboard-copy callback, process-local event counters,
last-stage/rejection/exception state and movement-report formatting. The saved
上フリックで長押し文字を入力 switch remains the same key and is initially OFF.
No substitute diagnostic toggle or background recorder is added.

The validation helpers now return booleans instead of producing diagnostic
reason strings. Gesture conditions, evaluation order, native cancellation,
owned-stream draining and direct/popup alternate selection remain unchanged.
The former diagnostic tests now assert visible input/ownership behavior and
absence of the diagnostic setting. Existing core gesture scenarios are retained.

Preserved upward-flick policy
The upward sector is +/-67.5 degrees from vertical, with Euclidean minimum
max(6px,0.24*shorterKeySide) and upward component minimum
max(0.5*radialMinimum,0.5*boundedTouchSlop). Small non-upward initial motion is
undecided until max(2*minimum,2*boundedTouchSlop), then yields to native/glide.
The 500ms pre-recognition limit uses current event time before history is read.
The first qualifying MOVE sends native CANCEL once; owned events remain with
the flick until UP. UP commits the original verified character once after
context/editor/actor validation. Direct DOWN-to-UP flicks remain supported.
Post-capture angle, travel, reversal and duration do not revoke recognition.
Invalid owned streams drain without handing unmatched events back to native.
Single-character native long presses and native popup default characters keep
the 1.8.13 resolution routes. Nothing maps a key label to a guessed digit.

Preservation contract
Pinned inputs are SwiftKey v1.8.13 and combined bundle v1.0.296, published at
0f751016f9e89ac2bbf1807d160cfe5a826e7404. The allowed changed MPP entries are:
- META-INF/MANIFEST.MF
- extensions/swiftkey_japanese.mpe
- classes.dex
- app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class
The only added archive entry is
app/hiro/swiftkey/patches/CandidateScrollPatch.class.

All other JVM patch classes, dictionary resources and other apps' payloads stay
byte-identical. Both Android and JVM loaders include the candidate hook. The
extension replaces the existing FlickSymbols family and adds CandidateScroll;
its other 56 existing runtime classes and existing input/settings hooks remain
unchanged. Loader preservation is verified at the unaffected class level since
the loader DEX itself necessarily changes to add the candidate patch.

Build
Use Java 17 for compilation/D8 and Java 21 for Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1814.py --download \
    --input-dir input --tools tools --work work --output output
Inputs and tools are SHA256-pinned. Compile-only output permits a fresh native
API review before full packaging. Two independent full builds must produce
exactly the same six release assets.

Verification and publication
The build verifies retained gesture behavior, candidate update behavior,
absence of diagnostic functionality, native API references, runtime inventory,
archive deltas and dictionary/resource preservation. Actual Morphe loaders must
discover the updated patch dependency and return the exact packaged extension.
Host fakes and static/native-hook inspection are separate evidence and do not
establish Android input feel or a full clean-original-APK patch application.

The workflow verifies reviewed source hashes and publisher regression tests,
performs two builds, matches exact artifact hashes and preservation constraints,
then publishes. The publisher verifies upload bytes and immutable raw MPP URLs
before advancing main/dev feeds together using head leases. Local-only validation
never publishes. All other app feed metadata and ULike policy are retained.

Target and use
Microsoft SwiftKey Beta 9.13.16.4 (versionCode 1236271168), unmodified APK/APKS.
Update Hiro Morphe Patches to 1.0.297, apply the SwiftKey patch and install with
the same signing settings as the existing modified app. Updating only the patch
source does not replace the installed keyboard. Preserve app data.
Expand a Japanese word-candidate list and scroll until more candidates appear.
The 上フリック診断をコピー row should be absent from layout/key settings; the
上フリックで長押し文字を入力 switch and its saved selection remain available.
