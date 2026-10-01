#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
mkdir -p build/core-test
mapfile -t sources < <(find src/main/java src/test/java -name '*.java' | sort)
compile() {
  if command -v javac >/dev/null 2>&1; then javac "$@";
  else java -m jdk.compiler/com.sun.tools.javac.Main "$@"; fi
}
compile --release 8 -Xlint:all -d build/core-test "${sources[@]}"
java -ea -cp build/core-test com.hiro.ulike.hdr.input.P010FrameReaderTest
mkdir -p build/adapter-test
mapfile -t adapter_test_sources < <(find src/main/java src/android/java src/test-stubs/java src/test-android/java -name '*.java' | sort)
compile --release 8 -Xlint:all -d build/adapter-test "${adapter_test_sources[@]}"
java -ea -cp build/adapter-test com.hiro.ulike.hdr.input.android.AndroidP010FrameReaderTest
if [[ -n "${ANDROID_JAR:-}" ]]; then
  [[ -f "$ANDROID_JAR" ]] || { echo 'ANDROID_JAR does not exist' >&2; exit 1; }
  mkdir -p build/android-sdk
  mapfile -t android_sources < <(find src/main/java src/android/java -name '*.java' | sort)
  compile --release 8 -Xlint:all -classpath "$ANDROID_JAR" -d build/android-sdk "${android_sources[@]}"
  echo 'Android adapter compile PASS against supplied ANDROID_JAR (no Android execution)'
else
  echo 'Android SDK compile NOT RUN: ANDROID_JAR unset; host doubles are not SDK validation'
fi
