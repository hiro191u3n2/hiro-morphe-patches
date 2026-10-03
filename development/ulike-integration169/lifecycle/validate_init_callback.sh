#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 4 ]]; then echo 'Usage: validate_init_callback.sh STOCK_APK BASELINE_MPP TOOLS PRIVATE_BUILD' >&2; exit 2; fi
SOURCE=$(cd "$(dirname "$0")" && pwd)
STOCK=$(realpath "$1"); BASELINE=$(realpath "$2"); TOOLS=$(realpath "$3")
mkdir -p "$4"; BUILD=$(realpath "$4")
JDK="$TOOLS/jdk21/jdk-21.0.12.1+1/bin"
mkdir -p "$BUILD/loader" "$BUILD/classes"
python3 - "$BASELINE" "$BUILD/loader" <<'PY'
import pathlib,sys,zipfile
target=pathlib.Path(sys.argv[2])/'app/hiro/ulike/patches/MethodContract.class'
target.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(sys.argv[1]) as z:target.write_bytes(z.read('app/hiro/ulike/patches/MethodContract.class'))
PY
CP="$TOOLS/morphe-1.16.jar:$BUILD/loader"
"$JDK/javac" -cp "$CP" -d "$BUILD/classes" "$SOURCE/../integration/MergePayloads.java" "$SOURCE/PrepareNativeInitCallback182.java"
"$JDK/java" -Xmx1g -cp "$BUILD/classes:$CP" PrepareNativeInitCallback182 "$STOCK" "$BUILD"
cp "$BUILD/NATIVE_INIT_CALLBACK_EVIDENCE.json" "$SOURCE/NATIVE_INIT_CALLBACK_EVIDENCE.json"
