SwiftKey Beta v1.8.12 / Hiro Morphe Patches v1.0.295

Purpose
Replace release-time whole-path validation with explicit ownership of a gesture
once an upward intention has been recognized. Release 1.8.11 could admit a wider
angle while still rejecting a later sample and letting native key sliding and
long-press handling run until UP. This version changes that event lifecycle.
The design problem is established in code; the user's exact device-level cause
of unchanged feel remains unproven without device execution evidence.

Gesture ownership
Initially the native DOWN and undecided MOVE events are delivered normally.
Recognize upward intent in the +/-67.5-degree sector from vertical upward using
the existing Euclidean minimum max(6px,0.24*shorterKeySide). Also require an
upward component of max(0.5*minimum,0.5*boundedTouchSlop), so mostly sideways
contact jitter cannot latch a flick merely by entering the wide angle sector.
Small non-upward initial motion is undecided until
max(2*minimum,2*boundedTouchSlop), then yields
to native/glide. The pre-recognition candidate ceiling remains 500ms, checked
against the current event timestamp before processing its historical points.
An old point delivered in an event after this ceiling does not bypass it.

On the first qualified MOVE, send one native CANCEL, recheck context and actor
identity, and own the remainder of the gesture. Preserve the original key and
its verified native secondary actor. Consume owned MOVE events and commit once
at UP. A short DOWN-to-UP gesture without MOVE can recognize and cancel at UP.

After ownership, later angle, path distance, reversal and elapsed time no longer
revoke the flick. Invalid context/editor/actor changes, unexpected pointers,
explicit CANCEL and resets still prevent insertion. A cancelled native stream
is drained rather than handed back mid-gesture, preventing duplicate commits.
Native long-press timers remain unchanged before recognition; the captured
stroke's native CANCEL stops its pending long-press timer.

This deliberately prioritizes upward-start flicks over glides that start by
meeting the same upward criterion and turn later. Horizontal-start glides keep
the existing route. This is a product behavior change, not an assertion that
all prior glide paths remain available. Physical-device feel and native engine
coexistence have not been verified.

Native route and evidence
Static inspection of the archived target APK traced native MOVE through
xd0/f0.f -> ef0/d.l -> tj0/e.U -> gf0/c.d, where key sliding can continue, and
native CANCEL through ef0/d.l -> tj0/e.E -> gf0/a.b -> fc0/n2.b -> oc0/m.b ->
oc0/p.b, which removes the pending timer. Native pointer completion may also
clear transient SHIFT/SYMBOL state, so post-cancel state/actor checks remain.
See tests/ownership_rethink_review.json for the independently inspected route
and explicit limits of the previous host fake. This is static evidence, not an
execution test on Android or a confirmed diagnosis of the user's last gesture.

Diagnostics and scope
Diagnostics distinguish recognition/capture from final character insertion and
identify the running helper as 1.8.12. Only process-local counts, fixed refusal
codes and relative movement/threshold numbers are included. No absolute touch
position, typed text, key labels, editor content or application names are logged.
Copying requires an explicit diagnostic preference click. Counts include normal
typing and cannot be treated as intentional flick failure rates. The saved
preference remains intact and is initially OFF.

Reference design
Previous primary-source research of Unexpected Keyboard and archived FlickBoard
informed separating gesture selection from release-time coordinate noise. The
SwiftKey-specific early-CANCEL lifecycle, 67.5-degree merged upper sector and
500ms pre-recognition ceiling are local choices, not claimed shared standards.
Reference URLs and adaptation limits are in tests/natural_flick_reference_review.json.

Preservation contract
Pinned inputs are the published v1.8.11 standalone and v1.0.294 combined bundles.
Both MPPs change only their manifest and the same SwiftKey extension. All eight
SwiftKey JVM loader classes, both Android loader DEX files, dictionary resources
and other apps' payload entries remain byte-identical. Existing runtime classes
outside the FlickSymbols helper family remain canonically identical.
Existing hook classes and the 59-class inventory remain unchanged. The emoji
search/AI creation display options added in v1.8.6 are retained.

Build
Use Java 17 for compilation/D8 and Java 21 for real Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1812.py --download \
    --input-dir input --tools tools --work work --output output
All baseline/tool downloads are pinned by SHA256. Compile-only output is for
fresh ABI inspection; it does not create release MPPs. Run the full build into
a second work/output directory and require all six release assets to match.

Checks
The build verifies exact archive deltas, preserved runtime classes, ownership,
setting and diagnostic host tests, actual patch discovery/selection/dependency
graphs, and the selected patch's extension supplier bytes with both Morphe
1.17.0-dev.6 and 1.19.1/Patcher2.0.1. The loader probe loads old and new bundles in
one JVM and compares the returned streams to the corresponding packaged DEX.
No clean-original-APK apply test or Android-device input/UI test is claimed.

Publication
The workflow verifies exact source hashes, executes two independent builds and
the publisher tests, and compares all release asset hashes to the reviewed
manifest. Publishing uploads and verifies release assets, creates immutable raw
MPP URLs, then advances main/dev Manager feeds together with head leases.
The publisher refuses a changed baseline and preserves other app update scopes
and the ULike policy files. No publication occurs during local-only verification.

Target and use
Microsoft SwiftKey Beta 9.13.16.4 (versionCode 1236271168).
Apply the updated SwiftKey patch to the unmodified APK/APKS in Morphe Manager,
using the same signing settings as the currently installed modified app. Updating
the Morphe source alone does not change the installed keyboard. Retain app data.
Enable 上フリックで長押し文字を入力 in the layout/key settings and attempt a
flick in Japanese QWERTY. The diagnostic's 1.8.12 heading identifies this runtime.
Keep the keyboard process alive between attempting a flick and copying diagnosis.
