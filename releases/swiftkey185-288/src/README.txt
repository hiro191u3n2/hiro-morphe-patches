SwiftKey v1.8.5 / Hiro Morphe v1.0.288 packaging restoration

The prior combined bundle contained SwiftKey's Android DEX metadata but omitted
all eight JVM loader classfiles. The Manager UI and its embedded JVM patch host
use different representations. The actual Morphe JVM loader selected zero
SwiftKey patches from the broken combined bundle. This build restores the eight
classfiles exactly from the pinned standalone v1.8.4 package.

No runtime code is recompiled. Existing classes.dex, the SwiftKey extension,
all dictionaries and every previous non-manifest ZIP entry remain byte-identical.
The standalone package changes only its manifest. The combined package changes
its manifest and adds only the eight missing SwiftKey JVM loader classes.

Build with Python 3 and Java 17 (JDK compiler module available). Set MORPHE_JAVA
to a Java 21 executable to run the actual Morphe patch-loader test:

  MORPHE_JAVA=/path/to/java21/bin/java python build185.py --download \
    --input-dir /tmp/swiftkey185-input --tools /tmp/swiftkey185-tools \
    --work /tmp/swiftkey185-work --output /tmp/swiftkey185-output

Every baseline and tool is checked against a fixed SHA-256. The regression test
uses the real loadPatchesFromJar implementation, compares the old/new full patch
inventories and the standalone/combined SwiftKey dependency graph, and requires
the requested SwiftKey patch to resolve exactly once in the corrected bundle.
It repeats this against Morphe Desktop 1.17.0-dev.6 and 1.19.1 (Patcher 2.0.1)
and requires the two SDK results to agree. Both SDK JARs are SHA-256 pinned.

Official Manager source used to identify the two loading paths:
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/app/src/main/java/app/morphe/manager/patcher/patch/PatchBundle.kt
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/patcher-core/src/main/kotlin/app/morphe/manager/patcher/host/HostMain.kt
https://github.com/MorpheApp/morphe-manager/blob/fc07b496fd18df502e00507972c4828df245b727/patcher-core/src/main/kotlin/app/morphe/manager/patcher/patch/PatchKeys.kt

The source ZIP includes the build, publication workflow, tests and deterministic
QA. Original application APK/APKS files and signing keys are not distributed.
These tests prove the distribution defect and its repair at loader selection;
they are not a full clean-APK application test or Android device validation.

The Manager source update must be followed by applying the SwiftKey patch to
the supported unmodified 9.13.16.4 input and installing the result with the same
signing key. Merely refreshing the source does not alter an installed keyboard.
