#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
WORK=$(cd "$HERE/../.." && pwd)
TOOLS="$WORK/models168/tools"
JDK="$TOOLS/jdk21/jdk-21.0.12.1+1/bin"
PRIVATE="$HERE/private_build"
mkdir -p "$PRIVATE/composer_tools"
CP="$TOOLS/morphe-1.16.jar:$HERE/../tool_baseline:$HERE/../tools:$PRIVATE/composer_tools"
"$JDK/javac" -cp "$CP" -d "$PRIVATE/composer_tools" "$HERE/PrepareComposerObservation171.java" "$HERE/VerifyComposerObservation171.java"
"$JDK/java" -Xmx2g -cp "$CP" PrepareComposerObservation171 "$WORK/models168/inputs/base.apk" "$PRIVATE"
"$JDK/java" -Xmx2g -cp "$CP" VerifyComposerObservation171 "$WORK/models168/inputs/base.apk" "$PRIVATE/composer-observation-partial.dex"
PYTHONPATH="$WORK/models168/python_deps" python3 "$WORK/hdr_rebuild167/core/android_composer_replay/verify.py"
python3 - "$HERE" "$WORK" <<'PY'
import hashlib,json,pathlib,sys
here,work=map(pathlib.Path,sys.argv[1:])
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
core=work/'hdr_rebuild167/core/android_composer_replay'
rows=[line.split('\t') for line in (here/'private_build/composer-method-contracts.tsv').read_text().splitlines()]
callers=[line.split('\t') for line in (here/'private_build/composer-native-callers.tsv').read_text().splitlines()]
assert len(rows)==13 and len(callers)==10
report={'schema':'ulike-composer-observation-transform-171-v1','stock_apk_sha256':sha(work/'models168/inputs/base.apk'),
 'source_sha256':{n:sha(here/n) for n in ['PrepareComposerObservation171.java','VerifyComposerObservation171.java','validate_composer.sh']},
 'core_qa_sha256':sha(core/'QA.json'),'methods':{a:{'original':b,'transformed':c} for a,b,c in rows},
 'exact_original_method_reconstruction':True,'hooked_normal_exits':26,'exception_cleanup_regions':15,
 'entire_stock_dex_direct_call_inventory':{'writers':10,'call_sites':10,'each_unique_wrapper_observed':True,'callers':[{'method':a,'offset':int(b),'native_name':c,'original_sha256':d} for a,b,c,d in callers]},
 'private_delta_sha256':sha(here/'private_build/composer-observation-partial.dex'),
 'compile_time_hooks_enabled':False,'candidate_installed':False,'all_state_mutation_coverage':False,'native_initialization_callback_correlated':False,
 'source_setup_restore_queue_barriers_implemented':False,'art_or_device_tested':False,
 'limits':['Direct DEX coverage is limited to ten native composer writers; reflection/native-internal/generic noncomposer/texture/message state is not covered.',
 'Observer accepts complete-array counts only; partial counts and nondefault generic-effect fields invalidate the journal.',
 'Native init request fingerprint does not prove SDK/environment equivalence or asynchronous callback completion.',
 'App initialization dispatch schedules additional work via task scheduler, Handler.post and Handler.postDelayed; none is a native/render completion receipt.',
 'Partial lifetime delta and composer delta overlap three methods; these deltas are alternatives, must never be blindly merged.']}
(here/'QA_COMPOSER_OBSERVATION.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS saved bounded composer transform QA; no native setup or restoration barrier claimed')
PY
