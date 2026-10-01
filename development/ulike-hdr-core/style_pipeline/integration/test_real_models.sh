#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 6 ]]; then
  echo 'Usage: test_real_models.sh Natural.model Purity.model libbytenn.so libeffect.so Natural.zip Purity.zip' >&2
  exit 2
fi
: "${JAVA_HOME:?Set JAVA_HOME to JDK21}"
: "${ANDROID_JAR:?Set ANDROID_JAR to SDK36 android.jar}"
: "${ORT_HOST_JAR:?Set ORT_HOST_JAR to onnxruntime-1.30.0.jar}"
integration_root=$(cd "$(dirname "$0")" && pwd)
core_root=$(cd "$integration_root/../.." && pwd)
integration_build=$(mktemp -d)
trap 'rm -rf "$integration_build"' EXIT
mkdir -p "$integration_build/classes"
mapfile -t integration_sources < <(rg --files "$core_root/android_model_runtime/src" \
  "$core_root/android_beauty_image/src" "$core_root/style_pipeline/src/main" \
  "$integration_root/src" "$integration_root/test" -g '*.java' | sort)
"$JAVA_HOME/bin/javac" --release 8 -Xlint:-options -cp "$ANDROID_JAR:$ORT_HOST_JAR" \
  -d "$integration_build/classes" "${integration_sources[@]}"
ORT_DISABLE_TELEMETRY=1 "$JAVA_HOME/bin/java" -cp "$integration_build/classes:$ORT_HOST_JAR" \
  com.hiro.ulike.style.integration.RealModelAdapterTest NATURAL_BLUSH "$1" "$3" "$4" "$5" > "$integration_build/natural.json"
ORT_DISABLE_TELEMETRY=1 "$JAVA_HOME/bin/java" -cp "$integration_build/classes:$ORT_HOST_JAR" \
  com.hiro.ulike.style.integration.RealModelAdapterTest PURITY2 "$2" "$3" "$4" "$6" > "$integration_build/purity.json"
python - "$integration_root" "$integration_build" "$@" <<'PY'
import hashlib,json,pathlib,sys
root,build=map(pathlib.Path,sys.argv[1:3])
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
cases=[json.loads((build/(style+'.json')).read_text()) for style in ['natural','purity']]
result={'schema':'ulike-actual-model-post-neural-adapter-1','status':'PASS','cases':cases,
 'analytic_fp32_output_bits_compared':2*512*384*3,'input_output_dimensions':[512,384],
 'geometry_and_makeup_samples':'explicit synthetic fixtures; not actual face/style equivalence',
 'real_neural_models_and_supplied_neural_masks':True,'android_device_execution':False,
 'hdr_integration':False,'full_style_reproduction_verified':False,
 'late_binding_error_outer_neural_and_adapter_abort_once':True,
 'telemetry_opt_out_before_runtime':True,
 'input_sha256':{name:sha(pathlib.Path(file)) for name,file in zip(['natural_model','purity_model','bytenn_library','effect_library','natural_style_zip','purity_style_zip'],sys.argv[3:])},
 'test_sha256':sha(root/'test/com/hiro/ulike/style/integration/RealModelAdapterTest.java'),
 'adapter_sha256':sha(root/'src/com/hiro/ulike/style/integration/PostNeuralStyleSink.java')}
(root/'QA_REAL_MODEL_ADAPTER.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({'status':result['status'],'real_models':len(cases),'analytic_fp32_output_bits_compared':result['analytic_fp32_output_bits_compared']}))
PY
