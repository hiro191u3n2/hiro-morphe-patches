SwiftKey Beta 1.8.28 / Hiro Morphe Patches 1.0.340

Candidate order retains existing usage learning, then native candidates, then
supplemental Japanese/English dictionary candidates. Existing emoji positioning
is unchanged. Japanese composition plus space selects and commits the highest
eligible reading-matched kanji candidate via the native candidate action, adding
no space. A subsequent space after commit uses the original action. English,
empty composition, or no ready eligible candidate falls through unchanged.
This is immediate conversion/commit, not a preview or candidate-cycling mode.

Build from the published SwiftKey 1.8.27 and combined 1.0.339 MPPs.
All input artifacts and build tools are pinned by URL and SHA-256 in build1828.py.
Java 17 compiles the sources; Java 21 executes the Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1828.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1828.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Both work/output pairs must be fresh and distinct. Each run rebuilds the runtime
and both loader representations, runs the behavior/native-hook fixtures, checks
native ABI evidence, and executes two actual Morphe loader/resource probes.
The source/archive inventory and all six output artifacts must match the pinned
manifest before publication. The CI workflow repeats both builds independently.

Existing dictionary files are preserved byte for byte. The runtime changes only
the enhance methods of WordCandidates and KatakanaEnglishCandidates, and adds
the SpaceConversion helper family. The loader changes only the main SwiftKey
provider bridges and adds SpaceConversionPatch. All other classes, methods,
dictionary assets, and bundled apps are checked against their published inputs.

The native reference is an archived, previously modified SwiftKey Beta 9.13.16.4
APK. Native method fixtures and ABI validation do not establish success on a
clean APK or behavior on an Android handset. QA retains both limitations.

Publication uses an immutable download commit, a prerelease, and atomic main/dev
feed commits protected by expected-HEAD leases. A newer active bundle or source
head stops publication. The existing ULike active policy is preserved exactly.
