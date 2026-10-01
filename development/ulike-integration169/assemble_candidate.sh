#!/usr/bin/env bash
# Private static integration checkpoint. Does not activate capture or publish anything.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
WORK=$(cd "$HERE/.." && pwd)
TOOLS="$WORK/models168/tools"
JDK="$TOOLS/jdk21/jdk-21.0.12.1+1"
export ULIKE_JDK_DIR="$JDK"
cd "$HERE/../.."
(cd "$TOOLS" && sha256sum -c "$HERE/integration/tool-SHA256SUMS")
python3 - "$HERE" "$WORK/hdr_rebuild167/core" <<'PY'
from pathlib import Path
import hashlib,sys,shutil,zipfile
r,core=map(Path,sys.argv[1:]);jar=r/'runtime_payload/onnxruntime-android-1.30.0-classes.jar'
assert hashlib.sha256(jar.read_bytes()).hexdigest()=='65e2e2d76d672253aaf0792a06c71cc40d09b0e2e251ccf925bee7175b91a1b0'
prior=r.parent/'model_export168_integration/dist/ULike_HQ_Texture_Online_v1.6.5.mpp'
assert hashlib.sha256(prior.read_bytes()).hexdigest()=='cb9c34d360b5f84aa97ebfcf0a852af6136b68035848037cef6126e131f93c28'
with zipfile.ZipFile(prior) as z:
 for entry in ('app/hiro/ulike/patches/MethodContract.class','ulike/methods.dex','ulike/runtime.dex'):
  target=r/'tool_baseline'/entry;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(entry))
# Only generated class outputs are cleared, preventing removed source classes from surviving.
for folder in ('runtime/classes','runtime/dex','vendor_dex'):
 p=r/folder
 if p.exists():shutil.rmtree(p)
 p.mkdir(parents=True)
sources=[]
for p in core.rglob('*.java'):
 if 'src' not in p.parts or any(x in p.parts for x in ('test','tests','review','android_runtime_package','qa')) or any(x.startswith('test-') for x in p.parts):continue
 sources.append(p.resolve())
sources+=list((r/'runtime/src').rglob('*.java'))
(r/'runtime/sources.txt').write_text('\n'.join(map(str,sorted(sources)))+'\n')
(r/'audit/source-compilation-sha256.tsv').write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'\t'+str(p.relative_to(r.parent)) for p in sorted(sources))+'\n')
PY
mkdir -p "$HERE/compile_stubs/classes" "$HERE/runtime/dex" "$HERE/vendor_dex"
"$JDK/bin/javac" --release 8 -cp "$TOOLS/android.jar" -d "$HERE/compile_stubs/classes" "$HERE/compile_stubs/com/hiro/ulike/OpticalZoom.java"
"$JDK/bin/javac" --release 8 -cp "$TOOLS/android.jar:$HERE/runtime_payload/onnxruntime-android-1.30.0-classes.jar:$HERE/compile_stubs/classes" -d "$HERE/runtime/classes" @"$HERE/runtime/sources.txt"
"$JDK/bin/jar" cf "$HERE/runtime/helpers.jar" -C "$HERE/runtime/classes" .
"$JDK/bin/java" -cp "$TOOLS/r8-8.3.37.jar" com.android.tools.r8.D8 --min-api 26 --lib "$TOOLS/android.jar" --classpath "$HERE/runtime_payload/onnxruntime-android-1.30.0-classes.jar" --classpath "$HERE/compile_stubs/classes" --output "$HERE/runtime/dex" "$HERE/runtime/helpers.jar"
"$JDK/bin/java" -cp "$TOOLS/r8-8.3.37.jar" com.android.tools.r8.D8 --min-api 26 --lib "$TOOLS/android.jar" --output "$HERE/vendor_dex" "$HERE/runtime_payload/onnxruntime-android-1.30.0-classes.jar"
CP="$TOOLS/morphe-1.16.jar:$HERE/tool_baseline:$HERE/tools"
"$JDK/bin/javac" -cp "$CP" -d "$HERE/tools" "$HERE/integration/MergePayloads.java" "$HERE/tools/PrepareHooks169.java" "$HERE/tools/VerifyHooks169.java" "$HERE/tools/VerifyUiBindings169.java"
"$JDK/bin/java" -Xmx2g -cp "$CP" PrepareHooks169 "$WORK/models168/inputs/base.apk" "$HERE/tool_baseline/ulike/methods.dex" "$HERE/tool_baseline/ulike/runtime.dex" "$HERE/deltas"
"$JDK/bin/java" -cp "$CP" VerifyHooks169 "$HERE/tool_baseline/ulike/runtime.dex" "$HERE/deltas/runtime-overrides.dex" 3
"$JDK/bin/java" -Xmx2g -cp "$CP" VerifyHooks169 "$WORK/models168/inputs/base.apk" "$HERE/deltas/methods-delta.dex" 1
"$JDK/bin/java" -Xmx2g -cp "$CP" VerifyUiBindings169 "$WORK/models168/inputs/base.apk" "$HERE/audit/ui-contracts.tsv"
python3 - "$HERE" <<'PY'
from pathlib import Path
import hashlib,sys
r=Path(sys.argv[1]);v=r/'vendor_dex/classes.dex'
rows=[('delta',r/'deltas/methods-delta.dex',r/'deltas/method-contracts.tsv'),('runtime',r/'deltas/runtime-overrides.dex',r/'deltas/runtime-allowlist.txt'),('runtime',r/'runtime/dex/classes.dex','-'),('vendor',v,hashlib.sha256(v.read_bytes()).hexdigest())]
(r/'components.tsv').write_text('\n'.join('\t'.join(map(str,row)) for row in rows)+'\n')
PY
bash "$HERE/integration/build.sh" "$WORK/models168/inputs/base.apk" "$WORK/model_export168_integration/dist/ULike_HQ_Texture_Online_v1.6.5.mpp" "$WORK/model_export168_integration/dist/Hiro_Morphe_Patches_v1.0.98.mpp" "$TOOLS" "$HERE/components.tsv" "$HERE/candidate.json" "$HERE/build" "$HERE/private_dist"
