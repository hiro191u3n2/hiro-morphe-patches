SwiftKey Beta 1.8.20 / Hiro Morphe Patches 1.0.303

Purpose
Add a persistent, five-level upward-flick sensitivity setting to the current published 1.8.19 runtime. The default Standard level retains the exact existing threshold calculation. Lower levels require longer motion to reduce accidental alternate insertion. No native hooks or patch-loader code changes are required.

Pinned baseline
SwiftKeyBeta_v2_SamsungEmoji_v1.8.19.mpp SHA256 ebba15ca5c7a794f4d2012403578c959b4a99b077ca792baa8b2ebba1f19921f
Hiro_Morphe_Patches_v1.0.302.mpp SHA256 a7260bfb338ff5b5ec0323ed689f320293f1b54b553678ba969f6fb7437ddf93
Runtime extension SHA256 20ff4b578fa05c0c6be545c0f23680f9cc2df1f12bb380a7f1a25e392f6ef864
Immutable distribution commit 3ad028f4ed3e619d7a4db5841ce217e652f5d7c4.
Both standalone and integrated baselines are pinned before any transformation.

Implementation
Only src/hiro/swiftkey/FlickSymbols.java is rebuilt. Existing upper and diagonal sectors, capture deadline, lifecycle checks, native actors, cancellation ownership and exactly-once insertion are preserved. The per-touch minimum movement snapshots the selected multiplier at DOWN. Selection never revokes an owned touch stream.
hiro_up_flick_sensitivity is an integer from 0 to 4; default 2. Labels: とても低い, 低い, 標準, 高い, とても高い. Minimum-distance multipliers: 2.0, 1.5, 1.0, 0.8, 0.65. Unknown, out-of-range or corrupt stored values fall back to Standard.
The existing preference screen gets one ordinary Preference after the current toggle. A platform AlertDialog provides a single-choice list, persistent selection and Cancel. Current sensitivity is reflected in the preference summary. The original toggle anonymous class keeps its class identity.

Preservation
Merge1820 replaces only the FlickSymbols family in the existing runtime. The baseline has 79 classes: 3 FlickSymbols-family classes and 76 unrelated classes. Every unrelated class is checked using canonical DEX serialization and smali. Existing public entrypoints and calls from the rest of the extension must remain valid.
CandidateScroll, JapaneseStart, PunctuationKeys, KeySwap, CandidateToolbar, EmojiToolbar and every other unrelated class retain the latest 1.8.19 code. Both MPPs change only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe. All Android/JVM loaders, dictionaries, other resources and unrelated app entries remain byte-identical.

Build
Use Java17 for javac/D8, and Java21 for both real Morphe loader probes via MORPHE_JAVA. Tool and input hashes are pinned in build1820.py. The builder has --download, --input-dir, --tools, --work, --output and --compile-only options.
Compile first with --compile-only, then run tests/audit_sensitivity_abi.py against the archived reference APK and the exact extension. The audit's JSON records its source, reference APK and extension hashes. Its native/Android callback checks and reference-resolution scope are explicit.
Run build1820.py normally to run the production host tests, package both MPPs, compare every archive entry, execute old/current Morphe loaders and emit the QA report, source archive and checksums. Repeat in an independent work/output directory and compare all six artifacts.

Verification and limits
Host tests use production Java with modeled Android/native objects. They exercise settings, movement thresholds, default equivalence, original gesture regressions and interactions with existing glide handling. They are not Android UI, ART or physical-touch tests.
Static ABI analysis uses the saved older modified APK plus a pinned Android35 platform jar. It verifies reference/access/opcode compatibility, callback implementations and required public entrypoints. A clean target APK was not available; full patch application to a clean original and handset use remain unverified.
Unchanged historical tests/reviews in this source archive provide provenance for preserved code. The current build only claims checks it actually runs; the QA report distinguishes the current host suite, byte preservation, real loader probes and the hash-pinned local static ABI evidence.

Publication
publish1820.py accepts an independently reviewed manifest with exact source and artifact hashes. The workflow verifies those pins, requires the active feed to remain 1.0.302, rebuilds twice, and verifies both outputs against reviewed hashes. It then publishes the release and immutable downloads, advances main/dev feeds atomically under expected-head leases, checks public metadata/download bytes, and saves a publication receipt. A moved baseline aborts rather than replacing newer work. No unrelated app metadata or active ULike release policy is changed.
