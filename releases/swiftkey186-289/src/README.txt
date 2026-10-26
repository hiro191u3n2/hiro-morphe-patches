SwiftKey Beta v1.8.6 / Hiro Morphe Patches v1.0.289

Purpose
Two independent, initially disabled settings in SwiftKey's layout/key settings hide
the emoji search row (including its submit arrow) and the AI creation button.
The selectors match the archived 9.13.16.4 application's own localized resources.
Other search panels and the alternate Copilot action are outside these selectors.

Preservation contract
Pinned inputs are the published v1.8.5 standalone and v1.0.288 combined bundles.
All existing 49 SwiftKey runtime classes remain present. KeySwap receives exactly
two static calls and JapaneseGlide.start receives exactly one static call; removing
these calls restores the complete canonical original smali. The other 47 classes
remain canonically identical. Only the new EmojiPanelUi family is compiled.
Both MPPs change only their manifest and the same SwiftKey extension. All eight
SwiftKey JVM loader classes, both Android loader DEX files, dictionaries and other
apps' payload entries remain byte-identical. The combined loader retains275 classes.

Build
Use Java17 for compilation/D8 and Java21 for real Morphe loader execution:
  MORPHE_JAVA=/path/to/java21/bin/java python build186.py --download \
    --input-dir input --tools tools --work work --output output
All baseline/tool downloads are pinned by SHA256. Run the build independently into
a second work/output directory and require all six release assets to match.

Checks
The build checks the exact archive delta, full runtime class inventory and smali
equality, three reversible hook insertions, UI behavioral host tests, and actual
SwiftKey patch discovery/selection/dependency graphs with both Morphe1.17.0-dev.6
and Morphe1.19.1/Patcher2.0.1. The 20 existing JVM patches and their metadata remain
present, and SwiftKey selection remains one in baseline, standalone and combined.
The checked-in native UI/ABI reports document local inspection of an archived
already-modified APK; CI binds those reports and source pins, but does not ship
that APK or replay the private-APK inspection. Host tests use framework doubles.
No clean-original-APK apply test or Android-device input/UI test is claimed.

Publication
The workflow verifies exact source hashes, executes two independent builds and
the publisher tests, and compares all release asset hashes to the reviewed
manifest. Publishing uploads and verifies release assets, creates immutable raw
MPP URLs, then advances main/dev Manager feeds together with head leases.
Other app update flags and ULike policy files are preserved.

Primary implementation references
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/patcher-core/src/main/kotlin/app/morphe/manager/patcher/host/HostMain.kt
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/patcher-core/src/main/kotlin/app/morphe/manager/patcher/PatchPipeline.kt
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/patcher-core/src/main/kotlin/app/morphe/manager/patcher/patch/PatchKeys.kt
