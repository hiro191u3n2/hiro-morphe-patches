SwiftKey Beta 1.8.30 / Hiro Morphe Patches 1.0.342

This release repairs space conversion when visible dictionary kanji are absent
from a cache-hit prediction Future. Native ib0/r.g caches the unenhanced list;
on an all-cache-hit request it skips both enhancement and the UI update. Thus
its Future is not guaranteed to equal the currently displayed candidate list.

SpaceConversion first reads the native candidate-bar container at model.g.b.g.e.
It selects the first current kanji candidate in that container with a matching
raw reading, preserving the original object and rank for native selection.
The holder, container, list, request, active input marker, cursor and composition
are checked again before commit. A usable visible candidate does not wait for
or flush a prediction Future. If no visible candidate qualifies, the previous
native-buffer submission and bounded 750ms Future fallback remain available.
Committed input, no candidate, errors and timeout use the original space action.

All prior candidate ranking, dictionary resources, runtime classes other than
SpaceConversion, and both patch loader representations are preserved. The
existing loader hook and public handle signature are unchanged.

Build from the published SwiftKey 1.8.29 and combined 1.0.341 MPPs.
All input artifacts and build tools are pinned by URL and SHA-256 in build1830.py.
Java 17 compiles sources; Java 21 executes the Morphe loader and DEX tools.

  MORPHE_JAVA=/path/to/jdk21/bin/java python build1830.py --download \
    --input-dir INPUT --tools TOOLS --work BUILD_ONE --output DIST_ONE
  MORPHE_JAVA=/path/to/jdk21/bin/java python build1830.py \
    --input-dir INPUT --tools TOOLS --work BUILD_TWO --output DIST_TWO

Both work/output pairs must be fresh and distinct. Each run rebuilds the runtime,
runs behavior and regression tests, checks native ABI evidence, and executes two
actual Morphe loader/resource probes. The source/archive inventory and all six
output artifacts must match the reviewed manifest before publication. CI repeats
both builds independently.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change.
The runtime changes only SpaceConversion; all other runtime classes, dictionary
assets, both loader representations and bundled apps are compared with published
inputs. The 92-class inventory remains fixed.

The native reference is an archived, previously modified SwiftKey Beta 9.13.16.4
APK. Native method fixtures and ABI verification do not establish success on a
clean APK or behavior on an Android handset. The prior user's input video is
not included in the source or distribution. QA retains these limitations.

Publication uses an immutable download commit, a prerelease, and atomic main/dev
feed commits protected by expected-HEAD leases. A newer active bundle or source
head stops publication. Existing policies for other apps are preserved exactly.
