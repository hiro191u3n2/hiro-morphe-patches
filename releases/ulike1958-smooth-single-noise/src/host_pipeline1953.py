#!/usr/bin/env python3
"""Actual .52/.53 save pipeline pixel/ownership equality without native GPU.

The .52 pipeline is pinned verbatim from its published sources. Both pipelines
execute the same deterministic captured pixels across every settings, rotation,
chroma and geometry combination. Android Bitmap, camera and primary NR surfaces
are explicit fixtures; this is no physical Galaxy or speed measurement.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
from host_common1953 import sources1953,source_pins
ROOT=Path(__file__).resolve().parent
BASELINE_SHA256='73378b9f6f3be7206525090ad8937c814de960162ead92d17681713c230b3616'

def run(cmd,log,timeout=180):
 p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=timeout)
 Path(log).write_text(p.stdout+p.stderr)
 if p.returncode:raise RuntimeError(p.stdout[-3000:]+p.stderr[-10000:])
 return p.stdout

def test(root,work,android=None,gpu_library=None):
 root=Path(root).resolve();out=Path(work)/'host-pipeline1953';out.mkdir(parents=True,exist_ok=True)
 source_pins(root)
 baseline=root/'tests/pipeline1953-reference/QualityPipeline1932.java'
 if hashlib.sha256(baseline.read_bytes()).hexdigest()!=BASELINE_SHA256:raise AssertionError('Changed .52 pipeline oracle')
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
 common=list((root/'tests/pipeline1942-fixtures').rglob('*.java'))+list((root/'tests/timing1947-fixtures').rglob('*.java'))
 common += [root/n for n in ('SpeedWorkers1935.java','Scheduling1944.java','NoiseCache1944.java','NativeSpeed1944.java','GpuInteger1949.java','QualityPixels1932.java','NativeMoire1951.java','PolicyCache1945.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java','ProcessingTiming1947.java','tests/PipelineQuality1942Test.java','tests/QualityGolden1947Test.java')]
 reports={};assertions=0
 for name,pipeline in [('baseline1952',baseline),('updated1953',root/'QualityPipeline1932.java')]:
  classes=out/name;classes.mkdir(exist_ok=True)
  run(compiler+['-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',classes,*sources1953(root,(common if name=='updated1953' else [p if p!=root/'ProcessingTiming1947.java' else root/'tests/pipeline1953-reference/ProcessingTiming1947.java' for p in common])+[pipeline])],out/(name+'-compile.log'),120)
  # "instrumented" executes completion, cross-shot timing and copy-failure contracts
  # against both actual published and updated implementations, not just a digest.
  report=json.loads(run([java,'-XX:ActiveProcessorCount=4','-Djava.library.path='+str(out/'unavailable'),'-cp',classes,'com.hiro.ulike.QualityGolden1947Test','instrumented'],out/(name+'-golden.log')))
  contracts=json.loads(run([java,'-XX:ActiveProcessorCount=4','-Djava.library.path='+str(out/'unavailable'),'-cp',classes,'com.hiro.ulike.PipelineQuality1942Test'],out/(name+'-ownership.log')))
  reports[name]={'golden':report,'ownership':contracts}
  assertions+=report['assertions']+contracts['assertions']
 if reports['baseline1952']['golden']['output_sha256']!=reports['updated1953']['golden']['output_sha256']:
  raise AssertionError('Published .52 and updated .53 pipeline pixels differ')
 # Actual optional-hint integration uses the real pool, with faulted injected hooks.
 workers=out/'workers';workers.mkdir(exist_ok=True)
 run(compiler+['-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',workers,root/'SpeedWorkers1935.java',root/'tests/SpeedWorkersHints1952Test.java'],out/'workers-compile.log',120)
 worker=json.loads(run([java,'-XX:ActiveProcessorCount=4','-cp',workers,'com.hiro.ulike.SpeedWorkersHints1952Test'],out/'workers.log'))
 assertions+=worker['assertions']
 gpu_integration=None
 if gpu_library:
  library=Path(gpu_library).resolve()
  if not library.is_file():raise AssertionError('Actual host GPU JNI library required')
  classes=out/'updated1953'
  run(compiler+['-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-cp',classes,'-d',classes,root/'tests/PipelineGpu1953Test.java'],out/'gpu-integration-compile.log',120)
  old_egl=os.environ.get('EGL_PLATFORM');old_sw=os.environ.get('LIBGL_ALWAYS_SOFTWARE')
  os.environ.update(EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
  try:
   gpu_integration=json.loads(run([java,'-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(library.parent),'-cp',classes,'com.hiro.ulike.PipelineGpu1953Test'],out/'gpu-integration.log',240))
  finally:
   for name,value in [('EGL_PLATFORM',old_egl),('LIBGL_ALWAYS_SOFTWARE',old_sw)]:
    if value is None:os.environ.pop(name,None)
    else:os.environ[name]=value
  assertions+=gpu_integration['assertions']
 result={'status':'passed','assertions':assertions,'baseline_source_sha256':BASELINE_SHA256,'baseline_version':'1.9.52',
  'same_output_pixels':True,'pixel_equivalence_to_baseline':True,'gpu_unavailable_cpu_fallback_exact':True,
  'captured_options_and_rotation_geometry_exact':True,'source_and_prepared_ownership_preserved':True,
  'copy_failure_fallback_preserved':True,'normalization_and_detail_call_contracts_preserved':True,
  'baseline':reports['baseline1952'],'updated':reports['updated1953'],'worker_hint_integration':worker,'gpu_bitmap_integration':gpu_integration,
  'physical_android_tested':False,'device_speedup_verified':False,
  'fixture_scope':'Actual .52 and .53 pipeline/worker code; Android Bitmap, primary NR, camera and unavailable OS service are explicit host fixtures.'}
 (out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--gpu-library');a=p.parse_args();print(json.dumps(test(ROOT,a.out,gpu_library=a.gpu_library),indent=2))
