SwiftKey Japanese Glide runtime repair — v1.8.4 / bundle v1.0.287

Build with Python 3 and a Java 17 runtime containing the JDK compiler module.
The pinned Morphe launcher also requires Java 21; keep Java 17 on PATH for
compilation and set MORPHE_JAVA to the absolute Java 21 executable path:

  python build184.py --download --input-dir /tmp/swiftkey184-input \
    --tools /tmp/swiftkey184-tools --work /tmp/swiftkey184-work \
    --output /tmp/swiftkey184-output

The downloader verifies every baseline and tool against the SHA-256 pins in
build184.py. Existing inputs with a different hash are rejected, not replaced.
All compiler sources are under src/, stubs/, tests/, and tools_java/.
Native/Android test stubs are never included in the extension DEX.

Only META-INF/MANIFEST.MF and extensions/swiftkey_japanese.mpe may change in
either MPP. The build replaces JapaneseGlide, JapaneseGlideDecoder and the
new JapaneseGlideCandidates class families. KeySwap has one setting-summary
string update; its canonical smali must match the original after restoring
that literal. Every other runtime class, patch loader, dictionary, emoji,
layout resource and non-SwiftKey bundle entry is verified unchanged.

The source archive contains deterministic source files, the replacement smali
and QA metadata. Raw tool/test logs remain beside the build outputs because
they can contain local paths, timing or tool banners. The baseline APK is not
included. Checks involving that private APK are static ABI evidence, not
proof of a clean-APK full patch application or Android/ART device operation.

publish184.py verifies independent reviewed hashes, source coverage and the
active feed before publishing. workflow.yml rebuilds in two separate work
directories and publishes only when both builds match the reviewed outputs.
Publishing updates the Manager source. The installed keyboard changes only
after patching its supported unmodified base and updating the installed app.
