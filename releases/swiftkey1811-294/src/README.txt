SwiftKey Beta v1.8.11 / Hiro Morphe Patches v1.0.294

Purpose
Recognize natural diagonal upward motion with a direction cone, a radial
minimum distance, and bounded coordinate wobble at release. A fixed horizontal
width limit no longer aborts an otherwise valid upward-diagonal path.

Policy
The entry cone is +/-60 degrees from vertical upward. After a prior qualifying
upward point, an exit margin of +/-65 degrees permits small angular wobble.
Direction is assessed outside a deadzone of max(radialMinimum, jitterSlop).
The scalar minimum remains max(6px, 0.24*shorterKeySide), but is now applied to
radial distance instead of only the vertical component.

UP-only coordinate fallback uses the immediately preceding qualified MOVE or
history point, not the furthest point of the gesture. The release delta must
be <= min(maxReverse, 0.75*minimum), its final radius must be >= 0.5*minimum,
and it must still be above the starting point. Invalid coordinates, downward
travel beyond its guard, the upward limit, state changes, or a previously
rejected path cannot be bypassed by this fallback.

The total gesture timeout is 500ms rather than 350ms. This is a chosen tuning
value, not an Android or keyboard-industry standard. Native long-press/popup
and editor/state guards remain in place. Native DOWN/MOVE handling and
UP-only CANCEL/commit timing are retained.

The maximum upward travel remains 1.25*keyHeight. The 1.8.10 bounded Android
ViewConfiguration.getScaledTouchSlop() floor for down/reverse jitter is kept:
clamp platform slop to [0, half the shorter key dimension], with zero fallback
on lookup failure; maxDown >= 0.12*keyHeight and maxReverse >=
max(4px, 0.21*shorterKeySide). Long paths and sideways movement continue to
yield to the existing native/glide path. This is verified with host doubles,
not with the actual native engine or an Android device.

Diagnostics and scope
Detailed refusal reasons and relative movement/threshold values are retained,
with angle and radial-distance reporting for the changed classification.
No absolute coordinates, typed text, key labels, editor content or application
names are included. Copying requires an explicit diagnostic preference click.
Counts include normal typing and cannot be treated as a flick failure rate.
The saved preference remains intact and initially OFF. New on-device input
feel, false positives and native glide coexistence are not confirmed.

Reference design
Angle-based selection and release-coordinate handling were informed by public
implementations; the exact 60/65-degree cones and 500ms timeout are our tuning
choices, not values copied from those projects or established standards.
See the independently reviewed reference evidence under tests when included.
Unexpected Keyboard reference:
https://github.com/Julow/Unexpected-Keyboard/blob/68ebaa1e3912741e3de138c7f0543412fefe119e/srcs/juloo.keyboard2/Pointers.java
FlickBoard archived GitHub reference (not a claim about current Codeberg code):
https://github.com/nightkr/flickboard/blob/00c4109bfbc5ed08285cdfffeb11a1d5c37939f9/app/src/main/java/se/nullable/flickboard/ui/Key.kt

Preservation contract
Pinned inputs are the published v1.8.10 standalone and v1.0.293 combined bundles.
Both MPPs change only their manifest and the same SwiftKey extension. All eight
SwiftKey JVM loader classes, both Android loader DEX files, dictionary resources
and other apps' payload entries remain byte-identical. Existing runtime classes
outside the FlickSymbols helper family remain canonically identical.
Existing hook classes and the 59-class inventory remain unchanged.
The emoji search/AI creation display options added in v1.8.6 are retained.

Build
Use Java17 for compilation/D8 and Java21 for real Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1811.py --download \
    --input-dir input --tools tools --work work --output output
All baseline/tool downloads are pinned by SHA256. Run the build independently into
a second work/output directory and require all six release assets to match.

Checks
The build verifies the exact archive delta, existing runtime preservation,
gesture/setting/diagnostic host tests, and actual SwiftKey patch discovery/selection
and dependency graphs with both Morphe1.17.0-dev.6 and Morphe1.19.1/Patcher2.0.1.
Review reports distinguish native API inspection from host framework doubles.
Neither host testing nor archived APK inspection is a device execution test.
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
using the same signing settings as the currently installed modified app.
Enable 上フリックで長押し文字を入力 in SwiftKey's layout/key settings, attempt
a diagonal upward flick in Japanese QWERTY, then click 上フリック診断をコピー
if needed. Keep the keyboard process alive between attempt and copy. Updating
the Morphe source alone does not change the installed keyboard. Retain app data;
the diagnostics themselves are only in process memory.
