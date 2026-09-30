#!/usr/bin/env bash
set -euo pipefail
SRC=$(cd "$(dirname "$0")" && pwd)
OLD_STANDALONE=$(realpath "$1"); OLD_INTEGRATED=$(realpath "$2"); TOOLS=$(realpath "$3")
mkdir -p "$4" "$5"; BUILD=$(realpath "$4"); DIST=$(realpath "$5")
mkdir -p "$BUILD/old" "$BUILD/helper" "$BUILD/helper-dex" "$BUILD/compile-stubs" "$BUILD/test-core" "$BUILD/newdex" "$BUILD/newpatch/app/hiro/ulike/patches"
(cd "$TOOLS" && sha256sum -c "$SRC/tool-SHA256SUMS")
python3 - "$OLD_STANDALONE" "$OLD_INTEGRATED" "$BUILD" <<'PY'
import sys,zipfile,hashlib
from pathlib import Path
s,i,b=sys.argv[1:];b=Path(b)
assert hashlib.sha256(Path(s).read_bytes()).hexdigest()=='537f4aa69058d8f65481d9556e2cb36be76f42235e44acf45c39859f9b0e8a7a'
assert hashlib.sha256(Path(i).read_bytes()).hexdigest()=='5615e67dfca1da9ccad4b6625f433588d005755e9aedca63bf6d3d205c26c46f'
with zipfile.ZipFile(s) as z:z.extractall(b/'old')
with zipfile.ZipFile(i) as z:(b/'integrated-old.dex').write_bytes(z.read('classes.dex'))
PY
python3 "$SRC/test/make_stubs.py" "$BUILD/test-stubs"
javac --release 8 -cp "$TOOLS/android.jar" -d "$BUILD/test-core" $(find "$BUILD/test-stubs" -name '*.java' | sort) "$SRC"/java/com/hiro/ulike/*.java "$SRC"/test/*.java
java -cp "$BUILD/test-core" DetailPixelsTest | tee "$DIST/pixel-tests.txt"
java -cp "$BUILD/test-core" EnhancedDetailTest | tee "$DIST/enhanced-tests.txt"
java -cp "$BUILD/test-core" PhotoDetailTest | tee "$DIST/control-tests.txt"
java -Xmx768m -cp "$BUILD/test-core" FullResolutionTest | tee "$DIST/full-resolution-test.txt"
javac --release 8 -cp "$TOOLS/android.jar" -d "$BUILD/compile-stubs" "$SRC"/stubs/com/hiro/ulike/*.java
javac --release 8 -cp "$TOOLS/android.jar:$BUILD/compile-stubs" -d "$BUILD/helper" "$SRC"/java/com/hiro/ulike/*.java
jar cf "$BUILD/helpers.jar" -C "$BUILD/helper" .
java -cp "$TOOLS/r8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$TOOLS/android.jar" --output "$BUILD/helper-dex" "$BUILD/helpers.jar"
javac -cp "$TOOLS/morphe-1.13.jar:$BUILD/old" -d "$BUILD" "$SRC"/{MergeRuntime,MergePatchDex,VerifyApplied,DumpDex}.java
java -cp "$BUILD:$TOOLS/morphe-1.13.jar:$BUILD/old" MergeRuntime "$BUILD/old/ulike/runtime.dex" "$BUILD/helper-dex/classes.dex" "$BUILD/runtime.dex" | tee "$DIST/runtime-verification.txt"
javac -cp "$TOOLS/asm.jar" -d "$BUILD" "$SRC/UpdatePatchStrings.java"
java -cp "$BUILD:$TOOLS/asm.jar" UpdatePatchStrings "$BUILD/old/app/hiro/ulike/patches/UlikeHqMaxPatch.class" "$BUILD/newpatch/app/hiro/ulike/patches/UlikeHqMaxPatch.class"
cp "$BUILD/old/app/hiro/ulike/patches/MethodContract.class" "$BUILD/newpatch/app/hiro/ulike/patches/"
jar cf "$BUILD/patch-classes.jar" -C "$BUILD/newpatch" .
JHOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}
java -cp "$TOOLS/r8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$JHOME" --classpath "$TOOLS/morphe-1.13.jar" --output "$BUILD/newdex" "$BUILD/patch-classes.jar"
java -cp "$BUILD:$TOOLS/morphe-1.13.jar:$BUILD/old" MergePatchDex "$BUILD/integrated-old.dex" "$BUILD/newdex/classes.dex" "$BUILD/integrated-new.dex" | tee "$DIST/integrated-preservation.txt"
python3 "$SRC/package_mpp.py" --old-standalone "$OLD_STANDALONE" --old-integrated "$OLD_INTEGRATED" --build "$BUILD" --output "$DIST"
