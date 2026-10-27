SwiftKey Beta v1.8.9 / Hiro Morphe Patches v1.0.292

Purpose
Add manual-copy, text-free diagnostics to the existing upward-flick setting.
Counters and refusal-stage codes distinguish an uncalled hook from blocked
eligibility or gesture checks. Diagnostics never include input text, key labels,
editor content or application names. Copying occurs only on preference click.
Tolerate only tiny float rounding at native normalized key boundaries.
The general cause of the reported device failure remains undetermined; this
release does not claim a confirmed device fix. Keep all v1.8.8 sensitivity values
and the existing saved, initially-off preference.

Preservation contract
Pinned inputs are the published v1.8.8 standalone and v1.0.291 combined bundles.
Both MPPs change only their manifest and the same SwiftKey extension. All eight
SwiftKey JVM loader classes, both Android loader DEX files, dictionary resources
and other apps' payload entries remain byte-identical. Existing runtime classes
outside the FlickSymbols helper family remain canonically identical.
Existing hook classes and the class inventory remain unchanged.
The emoji search/AI creation display options added in v1.8.6 are retained.

Build
Use Java17 for compilation/D8 and Java21 for real Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build189.py --download \
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
q upward once in Japanese QWERTY, then click 上フリック診断をコピー and report
the copied diagnostic. Keep the keyboard process alive between attempt and copy.
Updating the Morphe source alone does not change the installed keyboard.
Retain app data; the diagnostics themselves are only in process memory.
