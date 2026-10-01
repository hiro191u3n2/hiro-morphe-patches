#!/usr/bin/env bash
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to JDK21}"
: "${ANDROID_JAR:?Set ANDROID_JAR to SDK36 android.jar}"
: "${R8_JAR:?Set R8_JAR to r8-8.3.37.jar}"
style_root=$(cd "$(dirname "$0")" && pwd)
style_build=$(mktemp -d)
trap 'rm -rf "$style_build"' EXIT
mkdir -p "$style_build/classes" "$style_build/dex"
mapfile -t style_sources < <(rg --files "$style_root/src/main" -g '*.java' | sort)
"$JAVA_HOME/bin/javac" -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR" \
  -d "$style_build/classes" "${style_sources[@]}"
mapfile -t style_classes < <(rg --files "$style_build/classes" -g '*.class' | sort)
"$JAVA_HOME/bin/java" -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 26 \
  --lib "$ANDROID_JAR" --output "$style_build/dex" "${style_classes[@]}"
python - "$style_build/dex/classes.dex" "$ANDROID_JAR" "$R8_JAR" <<'PY'
import hashlib,json,pathlib,sys
paths=[pathlib.Path(p) for p in sys.argv[1:]]
b=paths[0].read_bytes()
if not b.startswith(b'dex\n'): raise RuntimeError('D8 did not create DEX')
print(json.dumps({'status':'PASS','dex_bytes':len(b),'dex_sha256':hashlib.sha256(b).hexdigest(),
                  'android_jar_sha256':hashlib.sha256(paths[1].read_bytes()).hexdigest(),
                  'r8_jar_sha256':hashlib.sha256(paths[2].read_bytes()).hexdigest(),
                  'device_execution_verified':False}))
PY
