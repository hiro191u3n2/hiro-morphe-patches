#!/usr/bin/env python3
"""Execute the new mandatory GPU paths plus isolated unchanged shader math.

Historical CPU-fallback route tests are not reused as production coverage: .65
intentionally rejects those routes. Required .65 suites execute current source,
exercise unsupported precision as failure, and never switch strict mode off.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os
ROOT=Path(__file__).resolve().parent
SUITES={
 'gpu_binary64_foundation1965':'tests1965/host_soft64_1965.py',
 'strong_shader_regression':'tests1964/strong_tiles1964.py',
 'gpu_model1965':'tests1965/host_model1965.py',
 'gpu_plan1965':'tests1965/host_plan1965.py',
 'gpu_policy1965':'tests1965/host_policy1965.py',
 'gpu_chroma1965':'tests1965/host_chroma1965.py',
 'strict_route1965':'tests1965/host_strict_route1965.py',
 'gpu_recovery1965':'tests1965/host_required_native1965.py',
 'camera_regression1965':'tests1965/host_camera_preservation1966.py',
 'gpu_trace_export1966':'tests1965/host_gpu_trace1966.py',
}
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def source_pins(root):
 return {p.relative_to(root).as_posix():sha(p) for p in root.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.glsl','.sh','.json','.dex','.txt','.tsv')}
def test(root,work,jdk=None,ndk=None):
 root,work=Path(root).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 baseline=Path(os.environ['ULIKE1965_BASELINE_MPP']).resolve()
 if baseline.stat().st_size!=1163379 or sha(baseline)!='cf6621ed1ec55c5b115516a04f897e380c785c56d9772ab48043b32c568ae48b':raise AssertionError('Exact published .64 input required')
 reviewed_sources=source_pins(root);reports={}
 for label,relative in SUITES.items():
  path=root/relative
  if not path.is_file():raise AssertionError('Required suite absent: '+relative)
  spec=importlib.util.spec_from_file_location(label+'_executed',path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
  report=mod.test(root,work,jdk,ndk)
  if not isinstance(report,dict) or report.get('status')!='passed' or type(report.get('assertions')) is not int or report['assertions']<=0:raise AssertionError('Executed suite report invalid: '+label)
  if report.get('physical_android_tested',report.get('physicalAndroidTested',False)) is not False:raise AssertionError('Host suite claims physical device: '+label)
  reports[label]=report
 route=reports['strict_route1965']
 if route.get('strict_gpu_required_routes_verified') is not True or route.get('actual_production_strict_routes_executed') is not True or route.get('cpu_image_processing_fallback_enabled') is not False or route.get('untreated_success_on_gpu_failure_enabled') is not False:raise AssertionError('Current mandatory production route/failure evidence required')
 if reports['gpu_recovery1965'].get('android_software_driver_refused') is not True:raise AssertionError('Production Android must reject known software GL drivers before dispatch')
 if reports['gpu_trace_export1966'].get('boundedGpuDiagnosticsIncluded') is not True or reports['gpu_trace_export1966'].get('frozenCameraSnapshotPreserved') is not True:raise AssertionError('Bounded GPU diagnostics must be included without changing frozen camera logs')
 # Each required suite is an executed gate; no unsupported suite is omitted.
 gpu=any(r.get('gpu_shader_execution_on_host') is True or r.get('actual_mesa_gles_execution') is True or r.get('actual_desktop_fp64_execution') is True for r in reports.values())
 if not gpu:raise AssertionError('No actual shader execution reported')
 sources=source_pins(root)
 if reviewed_sources!=sources:raise AssertionError('Production/test source changed during host suites')
 tests={name:digest for name,digest in sources.items() if name.startswith('tests1965/')}
 result={'schema':'ulike1966-mandatory-gpu-executed-host-v1','status':'passed','assertions':sum(r['assertions'] for r in reports.values()),
 'reports':reports,'sources':sources,'tests':tests,'baseline_mpp_sha256':sha(baseline),
 'gpu_shader_execution_on_host':True,'strict_gpu_required_routes_verified':True,
 'android_software_driver_refused':True,
 'production_gles_required_math_functional':all(reports[name].get('original_gles_required_math_executed') is True for name in ('gpu_model1965','gpu_plan1965','gpu_policy1965','gpu_chroma1965')),
 'host_pixel_equivalence_to_baseline':False,
 'pixel_comparison_scope':'Kernel fixtures and strict routes explicitly enumerated by each executed report; no complete Galaxy pipeline equivalence claim.',
 'historical_cpu_fallback_routes_intentionally_not_asserted':True,'production_source_unchanged_during_host_tests':True,
 'physical_android_tested':False,'device_speedup_verified':False,'device_quality_improvement_verified':False}
 (work/'result1966.json').write_text(json.dumps(result,sort_keys=True,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk,a.ndk),indent=2))
