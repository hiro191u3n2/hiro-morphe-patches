SwiftKey Beta 1.8.33 / Hiro Morphe Patches 1.0.345

This update preserves the current candidate order produced by SwiftKey and the
existing supplement pipeline. Global word-use counts must not move a historical
word ahead of the current context-ranked list. Previously used spellings absent
from that list are appended; existing learning records and persistence remain.
Space selection also accepts native non-prefix, non-verbatim kana predictions
in their existing positions, instead of skipping every kana-only candidate in
search of a later kanji spelling. Default/synthetic metadata is not proof of a
contextual prediction. True-verbatim native wrappers remain excluded.

The change has two exact runtime targets: the SpaceConversion class and the
FrequencyLearning.rank(List,l40.c,boolean) method body. The ranking donor's
other fields and methods are discarded. The merge preserves the original
FrequencyLearning class metadata, fields, every other method and all nested
classes. Restoring the original rank method must recreate the exact original
class. All 90 other runtime classes remain canonical-identical.

Baseline SpaceConversion Java reconstructs its full published class. Ranking
baseline Java reconstructs the exact published rank method, not the complete
FrequencyLearning class. Both baseline comparisons are required before merging.
ABI verification covers all changed SpaceConversion methods and rank, including
the actual native metadata interface and the preserved runtime store API.

Build from published SwiftKey 1.8.32 and combined 1.0.344 MPPs at immutable
commit c7cc4f193d27e20f2feeaa65ed6ef741ad59ec64. Input artifacts and tools are
pinned by URL and SHA-256 in build1833.py. Java17 compiles sources; Java21 runs
Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1833.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1833.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Fresh distinct work/output pairs are required. Initial ABI verification for
changed Java source requires --apk PATH to the pinned archived reference.
Verified native-abi.json is then frozen under runtime/tests for reproduction
without distributing application bytes. Both independent builds rerun behavior,
ranking, baseline-failure, cache, native-metadata and preservation tests; their
six deliverables must match the reviewed publication manifest.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change in
each MPP. All JVM and Android loader bytes are preserved from 1.8.32; no loader
compilation is performed. loader/retained1832.json carries the historical
separator-loader evidence with its published QA hash. Those seven native read
rewrites belong to 1.8.32 and are not presented as new work in this release.
The loader source is retained for review but not rebuilt. Current packages are
still tested with both real Morphe loader SDKs and final-resource probes.
All dictionaries, emoji order, unrelated resources and other-app patches stay
byte-identical. Source archives contain no application or signing-key bytes.

The native reference is a previously modified SwiftKey Beta 9.13.16.4 APK.
Host tests and native bytecode checks do not execute the language model or
establish which candidate the user's real text would produce. No clean-APK
full-application or new on-device success is claimed.

Publication checks source and artifact pins, the unchanged active baseline,
expected-HEAD leases and atomic main/dev feed commits. A newer active bundle
or source head stops publication. Other-app policies remain unchanged.
