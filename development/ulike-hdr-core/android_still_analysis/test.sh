#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${JAVA_HOME:?Set JAVA_HOME}"
: "${ANDROID_JAR:?Set ANDROID_JAR to SDK 36}"
build_dir="$(mktemp -d)"
trap 'rm -rf "$build_dir"' EXIT
"$JAVA_HOME/bin/javac" --release 17 -Xlint:all -Werror -d "$build_dir/host" src/main/java/com/hiro/ulike/hdr/stillanalysis/{SdkFaceSnapshot,StillMessageCollector,RecorderAdmission}.java src/test/java/com/hiro/ulike/hdr/stillanalysis/{StillAnalysisTest,RecorderAdmissionTest}.java
"$JAVA_HOME/bin/java" -cp "$build_dir/host" com.hiro.ulike.hdr.stillanalysis.StillAnalysisTest
"$JAVA_HOME/bin/java" -cp "$build_dir/host" com.hiro.ulike.hdr.stillanalysis.RecorderAdmissionTest
"$JAVA_HOME/bin/javac" --release 17 -Xlint:all -Werror -cp "$ANDROID_JAR" -d "$build_dir/android" ../android_face_backend/src/main/java/com/hiro/ulike/hdr/faceprobe/*.java src/main/java/com/hiro/ulike/hdr/stillanalysis/*.java
echo 'PASS actual SDK36 compile; native execution=false'
