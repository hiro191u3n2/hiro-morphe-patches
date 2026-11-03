SwiftKey Beta 1.8.29 / Hiro Morphe Patches 1.0.341

This release repairs the Japanese-space conversion runtime. The 1.8.28 candidate
priority changes are retained exactly; dictionary resources and patch loaders
remain byte-identical to the published inputs. See RELEASE_NOTES.txt for the
confirmed behavior and validation limitations.

The fixed runtime uses raw composition plus an active-composition marker rather
than the display string, and does not confuse the prediction request mode with
context text. Buffered native prediction work is submitted through the existing
SwiftKey action. Its Future result is awaited for at most 750 milliseconds, then
the first eligible current kanji candidate is committed through native selection.
Committed text, no candidate, errors and timeout return to the original space
action. The existing loader hook and its public runtime signature are unchanged.

Build from the published SwiftKey 1.8.28 and combined 1.0.340 MPPs.
All input artifacts and build tools are pinned by URL and SHA-256 in build1829.py.
Java 17 compiles the sources; Java 21 executes the Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1829.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1829.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Both work/output pairs must be fresh and distinct. Each run rebuilds the runtime,
runs the behavior tests, checks native ABI evidence, and executes two actual
Morphe loader/resource probes. The source/archive inventory and all six output
artifacts must match the pinned manifest before publication. The CI workflow
repeats both builds independently.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change.
The runtime changes only the SpaceConversion family; all other runtime classes,
dictionary assets, both loader representations, and bundled apps are checked
against their published inputs. The 92-class runtime inventory remains fixed.

The native reference is an archived, previously modified SwiftKey Beta 9.13.16.4
APK. Native method fixtures and ABI validation do not establish success on a
clean APK or behavior on an Android handset. QA retains both limitations.

Publication uses an immutable download commit, a prerelease, and atomic main/dev
feed commits protected by expected-HEAD leases. A newer active bundle or source
head stops publication. The existing ULike active policy is preserved exactly.
