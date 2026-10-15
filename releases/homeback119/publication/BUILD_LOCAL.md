# Local build and evidence commands for v1.1.9

Run after runtime, patch loader, tests and audit source updates are final. Use fresh work directories. The three supplied APKs are already patched; these commands only audit their DEX and do not modify, repatch or install them. A separate baseline runtime ABI audit compares against the exact published v1.1.8 runtime. Physical Android behavior remains untested.

The evidence audit sources must use the new release identities before these steps: `homeback119` schemas, baseline runtime v1.1.8 SHA-256 `d7f8bde517b31a814b49dffc76a522c994c3faac86549fbe39dc2b2bbe129ca8`, replacement v1.1.9. Remove superseded baseline117 reports; regenerate rather than rewriting their reported results. Layout fixture bytes are unchanged; the manifest schema is homeback119-layout-fixtures-v1.

```bash
set -euo pipefail
cd /workspace/scratch/827877861db7/homeback119
task_tools=/workspace/scratch/d238776e5025/repo-inspection/build-inputs/tools
task_base=/workspace/scratch/d238776e5025/dist118-final/Hiro_Morphe_Patches_v1.0.245.mpp
task_inputs=/workspace/scratch/d238776e5025/inputs
task_audit=/workspace/scratch/827877861db7/audit119-final
mkdir "$task_audit"
mkdir "$task_audit/runtime" "$task_audit/runtime-dex" "$task_audit/classes"
javac --release 8 -encoding UTF-8 -cp "$task_tools/android.jar" -d "$task_audit/runtime" src/runtime/*.java
python3 - <<'PYCODE'
from pathlib import Path
import importlib.util,zipfile
root=Path('/workspace/scratch/827877861db7/homeback119')
audit=root.parent/'audit119-final'
s=importlib.util.spec_from_file_location('legacy',root/'legacy/build.py');u=importlib.util.module_from_spec(s);s.loader.exec_module(u)
u.write_zip(audit/'runtime.jar',u.class_files(audit/'runtime'))
for name,mp in [('baseline118-runtime.dex','/workspace/scratch/d238776e5025/dist118-final/Hiro_Morphe_Patches_v1.0.245.mpp'),('provided116-runtime.dex','/workspace/scratch/d238776e5025/inputs/OneBack_Exit_NoRecents_v1.1.6.mpp')]:
 with zipfile.ZipFile(mp) as z:(audit/name).write_bytes(z.read('extensions/oneback_exit.mpe'))
PYCODE
java -cp "$task_tools/d8.jar" com.android.tools.r8.D8 --release --min-api 29 --lib "$task_tools/android.jar" --classpath "$task_tools/android.jar" --output "$task_audit/runtime-dex" "$task_audit/runtime.jar"
javac -encoding UTF-8 -cp "$task_tools/morphe.jar" -d "$task_audit/classes" evidence/ProvidedApkHookAudit.java evidence/BaselineRuntimeAbiAudit.java evidence/InstagramBackRouteAudit.java
java -Xmx3g -cp "$task_audit/classes:$task_tools/morphe.jar" ProvidedApkHookAudit "$task_inputs/apks/Instagram_v447.0.0.55.81(2147483647).apk" "$task_audit/provided116-runtime.dex" "$task_audit/runtime-dex/classes.dex" evidence/instagram_v447_input_audit.json
java -Xmx3g -cp "$task_audit/classes:$task_tools/morphe.jar" ProvidedApkHookAudit "$task_inputs/apks/Instagram-v447.0.0.55.81-patches-v3.10.0.apk" "$task_audit/provided116-runtime.dex" "$task_audit/runtime-dex/classes.dex" evidence/instagram_patches_v310_input_audit.json
java -Xmx3g -cp "$task_audit/classes:$task_tools/morphe.jar" ProvidedApkHookAudit "$task_inputs/apks/TikTok_v46.2.3(2147483647).apk" "$task_audit/provided116-runtime.dex" "$task_audit/runtime-dex/classes.dex" evidence/tiktok_v4623_input_audit.json
java -cp "$task_audit/classes:$task_tools/morphe.jar" BaselineRuntimeAbiAudit "$task_audit/baseline118-runtime.dex" "$task_audit/runtime-dex/classes.dex" evidence/baseline118_runtime_abi.json
python3 evidence/summarize_provided_apk_audit.py
python3 src/build.py --base "$task_base" --tools "$task_tools" --legacy legacy --work /workspace/scratch/827877861db7/build119-final --dist /workspace/scratch/827877861db7/dist119-final
```

Finish README.md and RELEASE_NOTES.txt with the actual findings and validation limitations before packaging. The following commands freeze verified local identities, package them, and run the full read-only local publication gate:

```bash
set -euo pipefail
cd /workspace/scratch/827877861db7/homeback119
task_dist=/workspace/scratch/827877861db7/dist119-final
task_base=/workspace/scratch/d238776e5025/dist118-final/Hiro_Morphe_Patches_v1.0.245.mpp
python3 publication/freeze_local.py --dist "$task_dist"
python3 publication/package.py --dist "$task_dist" --legacy legacy
python3 publication/publish.py --dist "$task_dist" --repo /workspace/scratch/d238776e5025/repo --base "$task_base" --local-only
```

CI must make two fresh builds, verify both MPP and QA bytes against the local freeze, then pass the unchanged publication preflight. Upload `publication/workflow.yml` as `.github/workflows/homeback119-publish.yml` and this source tree as `releases/homeback119`. Publishing is performed by that workflow only; do not bypass its current-source, two-build or active-baseline gates.
