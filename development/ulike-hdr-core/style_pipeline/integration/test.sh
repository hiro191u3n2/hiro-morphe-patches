#!/usr/bin/env bash
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to JDK21}"
: "${ANDROID_JAR:?Set ANDROID_JAR to SDK36 android.jar}"
: "${ORT_CLASSES_JAR:?Set ORT_CLASSES_JAR to onnxruntime-android-1.30.0-classes.jar}"
: "${R8_JAR:?Set R8_JAR to r8-8.3.37.jar}"
integration_root=$(cd "$(dirname "$0")" && pwd)
core_root=$(cd "$integration_root/../.." && pwd)
integration_build=$(mktemp -d)
trap 'rm -rf "$integration_build"' EXIT
mkdir -p "$integration_build/main" "$integration_build/test" "$integration_build/dex"
mapfile -t integration_sources < <(rg --files "$core_root/android_model_runtime/src" \
  "$core_root/android_beauty_image/src" "$core_root/style_pipeline/src/main" "$integration_root/src" -g '*.java' | sort)
"$JAVA_HOME/bin/javac" -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR" \
  -cp "$ORT_CLASSES_JAR" -d "$integration_build/main" "${integration_sources[@]}"
mapfile -t integration_tests < <(rg --files "$integration_root/test" -g '*.java' | sort)
"$JAVA_HOME/bin/javac" --release 8 -Xlint:-options -cp "$integration_build/main:$ANDROID_JAR:$ORT_CLASSES_JAR" \
  -d "$integration_build/test" "${integration_tests[@]}"
"$JAVA_HOME/bin/java" -cp "$integration_build/main:$integration_build/test" \
  com.hiro.ulike.style.integration.AdapterTest > "$integration_build/tests.json"
mapfile -t integration_classes < <(rg --files "$integration_build/main" -g '*.class' | sort)
"$JAVA_HOME/bin/java" -cp "$R8_JAR" com.android.tools.r8.D8 --min-api 26 --lib "$ANDROID_JAR" \
  --lib "$ORT_CLASSES_JAR" --output "$integration_build/dex" "${integration_classes[@]}"
python - "$integration_root" "$integration_build" "$ANDROID_JAR" "$ORT_CLASSES_JAR" "$R8_JAR" <<'PY'
import hashlib,json,pathlib,sys
root,build,android,ort,r8=map(pathlib.Path,sys.argv[1:])
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
result=json.loads((build/'tests.json').read_text())
dex=build/'dex/classes.dex'
if not dex.read_bytes().startswith(b'dex\n'):raise RuntimeError('missing DEX')
result.update({'schema':'ulike-post-neural-adapter-1','sdk36_compile':True,'dex_compile':True,
 'dex_bytes':dex.stat().st_size,'dex_sha256':sha(dex),'android_jar_sha256':sha(android),
 'ort_classes_jar_sha256':sha(ort),'r8_jar_sha256':sha(r8),
 'android_device_execution':False,'full_style_reproduction_verified':False,'hdr_integration':False,
 'source_sha256':{str(p.relative_to(root)):sha(p) for folder in ['src','test'] for p in sorted((root/folder).rglob('*.java'))}})
(root/'QA_INTEGRATION.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:result[k] for k in ['status','adapter_checks','sdk36_compile','dex_compile','dex_bytes']}))
PY
