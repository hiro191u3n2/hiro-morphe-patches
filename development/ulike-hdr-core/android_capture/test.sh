#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p build/host build/sdk
mapfile -t host_sources < <(find src/main/java src/test/java -name '*.java' | sort)
javac --release 8 -Xlint:all -d build/host "${host_sources[@]}"
java -ea -cp build/host com.hiro.ulike.hdr.capture.CaptureBoundaryTest
if [[ -n "${ANDROID_JAR:-}" ]]; then
  [[ -f "$ANDROID_JAR" ]] || { echo 'ANDROID_JAR missing' >&2; exit 1; }
  mapfile -t sdk_sources < <(find src/main/java src/android/java ../hdr_input/src/main/java ../hdr_input/src/android/java -name '*.java' | sort)
  javac --release 8 -Xlint:all -classpath "$ANDROID_JAR" -d build/sdk "${sdk_sources[@]}"
  echo 'Android API compile PASS (no device execution)'
else
  echo 'Android API compile NOT RUN: ANDROID_JAR unset'
fi
