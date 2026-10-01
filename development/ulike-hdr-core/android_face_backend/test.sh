#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME to a JDK}"
: "${ANDROID_JAR:?Set ANDROID_JAR to the actual SDK 36 android.jar}"
build_dir="$(mktemp -d)"
trap 'rm -rf "$build_dir"' EXIT
"$JAVA_HOME/bin/javac" --release 17 -Xlint:all -Werror -d "$build_dir/host" src/main/java/com/hiro/ulike/hdr/faceprobe/ProbeLedger.java src/main/java/com/hiro/ulike/hdr/faceprobe/StockSdkIdentity.java src/main/java/com/hiro/ulike/hdr/faceprobe/PixelOrientationEvidence.java src/main/java/com/hiro/ulike/hdr/faceprobe/OwnedRenderPixels.java src/test/java/com/hiro/ulike/hdr/faceprobe/ProbeLedgerTest.java src/test/java/com/hiro/ulike/hdr/faceprobe/OwnedRenderPixelsTest.java
"$JAVA_HOME/bin/java" -cp "$build_dir/host" com.hiro.ulike.hdr.faceprobe.ProbeLedgerTest
"$JAVA_HOME/bin/java" -cp "$build_dir/host" com.hiro.ulike.hdr.faceprobe.OwnedRenderPixelsTest
"$JAVA_HOME/bin/javac" --release 17 -Xlint:all -Werror -cp "$ANDROID_JAR" -d "$build_dir/android" src/main/java/com/hiro/ulike/hdr/faceprobe/*.java
echo 'PASS SDK36 compile; native execution=false'
