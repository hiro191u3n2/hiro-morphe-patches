SwiftKey Beta v1.8.13 / Hiro Morphe Patches v1.0.296

Purpose
Address the key-specific difference reported after 1.8.12: 1, 2 and 0 work with an
upward flick, while 3 through 9 do not. The previous resolver admitted only a
single direct native long-press character actor. Keys whose long press opens a
character popup were rejected before gesture recognition could begin.

Extend native alternate resolution to the popup's native default character
when the current configuration identifies one unambiguously. Do not infer a
digit from the QWERTY key label, invent a layout mapping, choose an arbitrary
first candidate, or admit non-character actions. Ambiguous or unsupported
native routes remain excluded. The new native review records the actual target
classes and selection route in tests/native_popup_review.json; host fakes and
ABI checks do not run that native
engine on Android.

Native default selection route
The archived target builds a single-character xc0/i secondary or a multi-character
xc0/f popup in fc0/s1.O/P. fc0/k1.m registers the direct fc0/d1 release actor only
for the single-character branch. The multi-character configuration is reachable
through the original key's restricted long-press-open action, without opening
it: oc0/m.b.c.b -> ic0/e.a -> fc0/b1(tag1) -> e60/b.a ->
androidx/lifecycle/h(tag7).b -> xc0/f.a -> fb0/f2.

The native default key is popup.d[popup.l], as used by xd0/g1 and ic0/q; it is
not assumed to be list index0. Resolve that child's currently permitted normal
release action through oc0/m.b.b / bf0/g.b and hc0/o.a. A verified fb0/l2 actor
uses native wb0/t0.b0 when invoked. Do not invoke the popup-opening callback or
use a direct InputConnection fallback. Native current restrictions and selected
key/action identity must still agree at capture and release.

This establishes a resolver coverage gap in the prior source that fits the
reported key-specific symptom. It does not establish the exact live language
variants, popup layout or failed branch on the user's device.

Unchanged gesture policy
This release retains 1.8.12's capture-on-movement ownership and thresholds.
The upward sector is +/-67.5 degrees from vertical, with Euclidean minimum
max(6px,0.24*shorterKeySide) and upward component minimum
max(0.5*radialMinimum,0.5*boundedTouchSlop). Small non-upward initial motion is
undecided until max(2*minimum,2*boundedTouchSlop), then yields to native/glide.
The 500ms pre-recognition limit uses current event time before history is read.

The first qualifying MOVE sends native CANCEL once; owned events remain with
the flick until UP. At UP, commit the original verified character once after
context/editor/actor validation. Direct DOWN-to-UP flicks remain supported.
Post-capture angle, travel, reversal and duration do not revoke recognition.
Invalid owned streams drain without handing unmatched events back to native.
An upward-start glide that meets the flick criteria remains a flick even if
it later turns; horizontal-start glide retains its existing route. Native
long press remains available before capture and is cancelled by capture.

Safety and diagnostics
Current native restrictions and original action identity are revalidated at
recognition and release. Existing view/layout/editor/password/pointer guards,
native popup-state guards and one-time input behavior are preserved. No changes
to composition or the SwiftKey conversion engine are introduced.

Diagnostics identify the runtime as 1.8.13 and retain process-local event,
recognition and insertion counts. Only fixed reason codes and relative movement
numbers are reported; no key labels, typed text, editor content, app names or
absolute touch positions are recorded. Copying requires an explicit settings
click. Counts include normal input, not only intended flick attempts.
The saved option is retained and is initially OFF.

Preservation contract
Pinned inputs are published SwiftKey v1.8.12 and combined bundle v1.0.295.
The only changed MPP entries are the manifest and SwiftKey extension. Both
loaders, all eight SwiftKey JVM patch classes, dictionary resources and other
apps' payloads remain byte-identical. Runtime classes outside the three-member
FlickSymbols helper family, its hook classes and 59-class inventory remain
unchanged. 1.8.12's gesture architecture remains the baseline rather than being
retuned to address this key-eligibility problem.

Build
Use Java 17 for compilation/D8 and Java 21 for Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1813.py --download \
    --input-dir input --tools tools --work work --output output
Inputs and tools are SHA256-pinned. Compile-only output allows a fresh native
ABI review before full packaging. Two independent full builds must produce
exactly the same six release assets.

Verification and publication
Tests cover the direct and popup-default resolver routes, refusal of ambiguous
or non-character routes, retained gesture behavior and native ownership using
explicit host doubles. Actual Morphe 1.17.0-dev.6 and 1.19.1/Patcher2.0.1 loaders
must discover the same patch/dependencies and return the extension bytes from
the exact selected MPP. Static target native inspection and host tests are
separate evidence; neither establishes on-device execution or input feel.

The workflow verifies reviewed source hashes, publisher regression tests,
two builds, exact artifact hashes and preservation constraints. The publisher
uploads verified assets, creates immutable raw MPP URLs, then advances main/dev
feeds together under head leases. Local-only validation never publishes.
No Android-device test or full clean-original-APK apply test is claimed.

Target and use
Microsoft SwiftKey Beta 9.13.16.4 (versionCode 1236271168), unmodified APK/APKS.
Apply the new patch in Morphe Manager and install with the same signing settings
as the existing modified app. Updating the source alone does not replace the
installed keyboard. Preserve app data. Enable 上フリックで長押し文字を入力 in
layout/key settings and try 3 through 9 in Japanese QWERTY. The diagnostic heading
must show 1.8.13. Keep the keyboard process alive between a test and copying it.
