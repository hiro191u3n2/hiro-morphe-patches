# Maps bicycle-marker build toolchain

The four JARs were recovered from their official publishers. Every JAR matches
the SHA-256 pinned by the preceding Maps builds; no build pin was changed.
The machine-readable record is `TOOLCHAIN_Maps_Bikes100.json`.

| Local name | Official distribution | Bytes | SHA-256 |
| --- | --- | ---: | --- |
| `morphe.jar` | Morphe Desktop 1.17.0-dev.6 | 46,589,795 | `b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58` |
| `morphe-2.2.jar` | Morphe Desktop 2.0.0, including Patcher 2.2.0 | 46,648,333 | `855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7` |
| `android.jar` | Android SDK Platform 35 revision 2 | 27,092,450 | `4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b` |
| `r8.jar` | D8/R8 8.7.18 full distribution | 51,023,743 | `badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa` |

## Official sources

- Morphe 1.17.0-dev.6: <https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar>
- Morphe 2.0.0: <https://github.com/MorpheApp/morphe-desktop/releases/download/v2.0.0/morphe-desktop-2.0.0-all.jar>
- Android platform archive: <https://dl.google.com/android/repository/platform-35_r02.zip>
- Android platform repository metadata: <https://dl.google.com/android/repository/repository2-3.xml>
- Full R8 distribution: <https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar>

The Android platform archive was also checked against the SHA-1 published in
Google's SDK repository metadata:
`0bb560a90a7a2cbd0dd8348224d518b638fe7949`.
Only its `android.jar` is a build input. The Maven R8 artifact is a different
distribution and does not match the previous build pin; it was not substituted.

## Java runtime and compiler

The runtime is the official Eclipse Temurin JRE 21.0.13+9 Linux x64 archive:
<https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jre_x64_linux_hotspot_21.0.13_9.tar.gz>.
Its SHA-256 was checked against the official GitHub release asset digest:
`a535a1df9c6284d9618872984e026ae0352d1f7e9e0605425c0e6114f47351dd`.
The preceding session used Zulu Java 21, so the Java distribution vendor changed;
the four pinned build/analysis JARs did not change.

`bin/javac` invokes the installed OpenJDK 17.0.20 compiler through
`/usr/bin/java com.sun.tools.javac.Main`. Production code is compiled with
`--release 8`, and the runtime compilation uses the pinned Android platform JAR.
`bin/javap` uses the same installed JDK's class inspection entry point.

The `java`, `javac`, and `javap` wrappers add `-XX:-UsePerfData`. This avoids a
HotSpot shared temporary file collision observed when concurrent Java processes
in this environment receive the same visible process ID. It does not alter the
compiled application bytecode.

The JSON record contains successful Java, javac, Morphe CLI, and D8 version
checks. Full patch release validation is recorded separately in
`QA_Maps_Hide_Bicycle_Marks_v1.0.0.json`; tool startup checks alone do not establish
that a patch works on Android.
