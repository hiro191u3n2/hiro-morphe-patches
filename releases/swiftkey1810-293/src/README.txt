SwiftKey Beta v1.8.10 / Hiro Morphe Patches v1.0.293

Purpose
Use Android ViewConfiguration.getScaledTouchSlop() as a bounded floor for
small downward drift and reverse travel during an upward flick. Clamp that
platform value to [0, half the shorter key dimension]. If lookup fails, use
zero and therefore retain the previous limits. maxDown remains at least
0.12*keyHeight; maxReverse remains at least max(4px, 0.21*shorterKeySide).
Do not change the minimum upward distance, final upward dominance, maximum
upward distance, horizontal limit, 350ms timeout, state guards, native touch
ownership, or UP-only commit timing. This preserves the existing handoff of
long or sideways paths to native/glide handling.

Split PATH_CURRENT and PATH_HISTORY into NON_FINITE, HORIZONTAL, DOWN,
UP_LIMIT and REVERSE subreasons. Retain only relative movement/limit values
from the failed path for diagnostics, never absolute coordinates, typed text,
key labels, editor content or application names. Clipboard changes require
an explicit diagnostic preference click. Inactive resets retain diagnostics.

Observed PATH_CURRENT establishes that an eligible candidate was cancelled by
a path guard. It does not establish which guard failed, why 1.8.7 reportedly
worked, or that this change fixes the actual device. All touch/rejection
counts include normal typing and cannot be treated as a flick failure rate.
The saved preference remains intact and initially OFF.

Preservation contract
Pinned inputs are the published v1.8.9 standalone and v1.0.292 combined bundles.
Both MPPs change only their manifest and the same SwiftKey extension. All eight
SwiftKey JVM loader classes, both Android loader DEX files, dictionary resources
and other apps' payload entries remain byte-identical. Existing runtime classes
outside the FlickSymbols helper family remain canonically identical.
Existing hook classes and the 59-class inventory remain unchanged.
The emoji search/AI creation display options added in v1.8.6 are retained.

Build
Use Java17 for compilation/D8 and Java21 for real Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build1810.py --download \
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
q upward once in Japanese QWERTY, then click 上フリック診断をコピー if needed.
Keep the keyboard process alive between attempt and copy. Updating the Morphe
source alone does not change the installed keyboard. Retain app data; the
diagnostics themselves are only in process memory.
