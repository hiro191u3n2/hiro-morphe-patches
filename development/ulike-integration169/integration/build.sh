#!/usr/bin/env bash
set -euo pipefail
SOURCE=$(cd "$(dirname "$0")" && pwd)
if [[ $# != 8 ]]; then echo 'Usage: build.sh STOCK OLD_STANDALONE OLD_INTEGRATED TOOLS SPECS RELEASE_JSON BUILD DIST' >&2; exit 2; fi
STOCK=$(realpath "$1"); OLD_S=$(realpath "$2"); OLD_I=$(realpath "$3"); TOOLS=$(realpath "$4"); SPECS=$(realpath "$5"); RELEASE=$(realpath "$6")
mkdir -p "$7" "$8"; BUILD=$(realpath "$7"); DIST=$(realpath "$8")
JDK_DIR=${ULIKE_JDK_DIR:-"$TOOLS/jdk21-clean"}
JAVA="$JDK_DIR/bin/java"; JAVAC="$JDK_DIR/bin/javac"; JAR="$JDK_DIR/bin/jar"
mkdir -p "$BUILD/old" "$BUILD/java" "$BUILD/newpatch/app/hiro/ulike/patches" "$BUILD/newdex"
(cd "$TOOLS" && sha256sum -c "$SOURCE/tool-SHA256SUMS")
python3 - "$OLD_S" "$OLD_I" "$BUILD" "$RELEASE" <<'PY'
import sys,zipfile,json,hashlib
from pathlib import Path
s,i,b,r=sys.argv[1:];b=Path(b);r=json.loads(Path(r).read_text())
assert hashlib.sha256(Path(s).read_bytes()).hexdigest()=='cb9c34d360b5f84aa97ebfcf0a852af6136b68035848037cef6126e131f93c28'
assert hashlib.sha256(Path(i).read_bytes()).hexdigest()=='7f5d3baba1083ea1343e01ba7387216faf05d3c0e382e2435fccd0e939b2ce87'
with zipfile.ZipFile(s) as z:z.extractall(b/'old')
with zipfile.ZipFile(i) as z:(b/'integrated-old.dex').write_bytes(z.read('classes.dex'))
(b/'description.txt').write_text(r['patch_description'])
asset_source=Path(r['asset_payload_dir']) if r.get('asset_payload_dir') else b/'old/ulike'
asset_rows=[line.split('\t') for line in (asset_source/'assets.tsv').read_text().splitlines() if line]
old_rows=[line.split('\t') for line in (b/'old/ulike/assets.tsv').read_text().splitlines() if line]
assert len(asset_rows)==len(old_rows)==3
(b/'assets').mkdir(exist_ok=True)
for old,row in zip(old_rows,asset_rows):
 assert len(row)==4 and old[:2]==row[:2] and old[3]==row[3], 'Asset target/original contract changed'
 relative=Path(row[3]).relative_to('ulike')
 data=(asset_source/relative).read_bytes()
 assert hashlib.sha256(data).hexdigest()==row[2], 'Asset payload hash mismatch'
 (b/relative).write_bytes(data)
(b/'assets.tsv').write_bytes((asset_source/'assets.tsv').read_bytes())
PY
CP="$TOOLS/morphe-1.16.jar:$BUILD/old"
"$JAVAC" -cp "$CP" -d "$BUILD/java" "$SOURCE/MergePayloads.java" "$SOURCE/VerifyApplied.java" "$SOURCE/VerifyHelperReferences.java"
"$JAVA" -Xmx2g -cp "$BUILD/java:$CP" MergePayloads merge "$STOCK" "$BUILD/old/ulike/methods.dex" "$BUILD/old/ulike/methods.tsv" "$BUILD/old/ulike/runtime.dex" "$SPECS" "$BUILD" 159 1073 | tee "$DIST/payload-verification.txt"
"$JAVA" -Xmx2g -cp "$BUILD/java:$CP" VerifyHelperReferences "$BUILD/old/ulike/methods.dex" "$BUILD/old/ulike/runtime.dex" "$BUILD/methods.dex" "$BUILD/runtime.dex" | tee "$DIST/helper-reference-verification.txt"
"$JAVAC" -cp "$TOOLS/asm-9.8.jar" -d "$BUILD/java" "$SOURCE/UpdatePatchStrings.java"
"$JAVA" -cp "$BUILD/java:$TOOLS/asm-9.8.jar" UpdatePatchStrings "$BUILD/old/app/hiro/ulike/patches/UlikeHqMaxPatch.class" "$BUILD/newpatch/app/hiro/ulike/patches/UlikeHqMaxPatch.class" 'v1.6.5：' "$BUILD/description.txt"
cp "$BUILD/old/app/hiro/ulike/patches/MethodContract.class" "$BUILD/newpatch/app/hiro/ulike/patches/"
cp "$BUILD/old/app/hiro/ulike/patches/NativeNv21EffectFlag.class" "$BUILD/newpatch/app/hiro/ulike/patches/"
cp -R "$SOURCE/../runtime_payload/helper_classes/app" "$BUILD/newpatch/"
cp -R "$SOURCE/../runtime_payload/ulike169" "$BUILD/"
cp "$SOURCE/../runtime_payload/resourceitems.tsv" "$BUILD/ulike169/resourceitems.tsv"
"$JAR" cf "$BUILD/patch-classes.jar" -C "$BUILD/newpatch" .
"$JAVA" -cp "$TOOLS/r8-8.3.37.jar" com.android.tools.r8.D8 --min-api 26 --lib "$JDK_DIR" --classpath "$TOOLS/morphe-1.16.jar" --output "$BUILD/newdex" "$BUILD/patch-classes.jar"
"$JAVA" -cp "$BUILD/java:$CP" MergePayloads loaders "$BUILD/integrated-old.dex" "$BUILD/newdex/classes.dex" "$BUILD/integrated-new.dex" | tee "$DIST/integrated-preservation.txt"
python3 "$SOURCE/package_mpp.py" --old-standalone "$OLD_S" --old-integrated "$OLD_I" --build "$BUILD" --release "$RELEASE" --output "$DIST" > "$DIST/package-verification.txt"
python3 - "$BUILD" "$SOURCE" <<'PY'
import sys,json
from pathlib import Path
b=Path(sys.argv[1]);sys.path.insert(0,sys.argv[2]);import package_mpp
paths=[b/n for n in ('methods.dex','methods.tsv','runtime.dex','assets.tsv','newdex/classes.dex','integrated-new.dex')]+list((b/'assets').glob('*.bin'))+list((b/'ulike169').rglob('*'))+list((b/'newpatch').rglob('*.class'))
(b/'payload-manifest.json').write_text(json.dumps({str(p.relative_to(b)):package_mpp.sha(p.read_bytes()) for p in sorted(paths) if p.is_file()},indent=2)+'\n')
PY
echo 'PASS packaging. Morphe application and APK verification remain separate required gates.'
