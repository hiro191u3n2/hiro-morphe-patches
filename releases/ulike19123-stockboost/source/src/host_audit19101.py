#!/usr/bin/env python3
"""Audit immutable .118 history plus actual .119 arithmetic and integration.

Historical inversions are not relabeled as coverage of changed .119 Java. The
new implementation is executed separately and every current input is pinned.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess,sys,time,urllib.request

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);sys.modules[name]=m;spec.loader.exec_module(m);return m

def source_pins(source):
 return {p.relative_to(source).as_posix():sha(p) for p in sorted(source.rglob('*')) if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts and p.suffix in ('.java','.py','.c','.h','.comp','.glsl','.sh','.txt','.json','.dex','.tsv','.md')}

def test(source,work,jdk=None,ndk=None,baseline_report=None):
 source,work=Path(source).resolve(),Path(work).resolve()
 if work.exists():raise AssertionError('Fresh host audit work directory required')
 work.mkdir(parents=True);began=time.monotonic()
 jdk=Path(jdk or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
 ndk=Path(ndk or os.environ.get('ULIKE_NDK_HOME') or source.parent/'tools/android-ndk-r27c').resolve()
 os.environ['PATH']=str(jdk/'bin')+os.pathsep+os.environ.get('PATH','');os.environ['ULIKE_JDK_HOME']=str(jdk);os.environ['ULIKE_NDK_HOME']=str(ndk)
 os.environ['EGL_PLATFORM']='surfaceless';os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
 sys.path.insert(0,str(source));import build19101,baseline_identity19119
 import numpy as np
 if np.__version__!='2.3.5':raise AssertionError('Pinned shader-proof NumPy2.3.5 required')
 baseline_identity19119.verify(source)
 pins=build19101.source_pins();pubpins=build19101.publication_pins()
 baseline_source=source.parents[1]/'ulike19118-pipeline/src'
 restore=module('restore_baseline118_inputs',source/'tests19119/restore_baseline118_inputs.py')
 choices=[baseline_source.parent/'dist'/restore.SINGLE_NAME,Path(os.environ.get('ULIKE_BASELINE118_MPP','/nonexistent')),Path(os.environ.get('ULIKE_TOOLS_DIR',str(source.parent/'tools'))).parent/'input'/restore.SINGLE_NAME,Path.cwd()/'.ci-ulike19119/input'/restore.SINGLE_NAME]
 single=next((p for p in choices if p.is_file()),None)
 if single is None:
  single=work/restore.SINGLE_NAME;ref=build19101.BASE_REFS[restore.SINGLE_NAME]
  with urllib.request.urlopen('https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/'+ref+'/downloads/'+restore.SINGLE_NAME,timeout=240) as response:raw=response.read()
  if len(raw)!=restore.SINGLE_BYTES or hashlib.sha256(raw).hexdigest()!=restore.SINGLE_SHA256:raise AssertionError('Downloaded baseline input pin differs')
  single.write_bytes(raw)
 restored=restore.restore(baseline_source,single)
 (work/'baseline-inputs.json').write_text(json.dumps(restored,indent=2)+'\n')
 baseline_pins=source_pins(baseline_source)
 if baseline_report is None:
  baseline_work=work/'immutable118';cmd=[sys.executable,str(baseline_source/'host_audit19101.py'),'--source',str(baseline_source),'--work',str(baseline_work),'--jdk',str(jdk),'--ndk',str(ndk)]
  with (work/'baseline-execution.log').open('w') as out:subprocess.run(cmd,stdout=out,stderr=subprocess.STDOUT,check=True)
  baseline_report=baseline_work/'result.json'
 baseline_report=Path(baseline_report).resolve();legacy=json.loads(baseline_report.read_text())
 if legacy.get('schema')!='ulike19101-host-audit-v1' or legacy.get('status')!='passed' or not legacy.get('source_stability_verified'):raise AssertionError('Executed complete immutable baseline report required')
 if legacy.get('current_source_sha256')!=baseline_pins:raise AssertionError('Baseline report differs from immutable118 source')
 if source_pins(baseline_source)!=baseline_pins:raise AssertionError('Baseline source changed')
 (work/'immutable118-result.json').write_bytes(baseline_report.read_bytes())
 tests={name:{**r,'regression_source_scope':'immutable_v118_baseline_regression'} for name,r in legacy['tests'].items()}
 for name,path in [('noise_arithmetic119','tests19119/arithmetic/host_arithmetic19119.py'),('noise_integration119','tests19119/integration/host_integration19119.py')]:
  r=module('audit_'+name,source/path).test(source,work/name,jdk,ndk)
  if r.get('status')!='passed' or type(r.get('assertions')) is not int or r['assertions']<=0:raise AssertionError('Empty/failed current group '+name)
  tests[name]=r;print('PASS '+name+' assertions='+str(r['assertions']),flush=True)
 import candidate_evidence19119
 def need(ok,message):
  if not ok:raise AssertionError(message)
 candidate_evidence19119.positive(tests,source,need)
 evidence_negatives=candidate_evidence19119.negative_controls(tests,source)
 if pins!=build19101.source_pins() or pubpins!=build19101.publication_pins():raise AssertionError('Current source/publication changed during execution')
 result=dict(schema='ulike19101-host-audit-v1',status='passed',assertions=sum(r['assertions'] for r in tests.values()),tests=tests,
  current_source_sha256=pins,current_publication_sha256=pubpins,source_stability_verified=True,
  baseline_source_sha256=baseline_pins,baseline_host_report_sha256=sha(baseline_report),baseline_host_report=legacy,
  candidate_evidence_negative_controls=evidence_negatives,baseline_source_scope='immutable_v118_baseline_regression',current_source_groups=['noise_arithmetic119','noise_integration119'],
  host_python_version=sys.version.split()[0],shader_proof_numpy_version=np.__version__,host_elapsed_seconds=round(time.monotonic()-began,3),
  physical_android_tested=False,device_speedup_verified=False,first_shot_gpu_verified_on_physical_android=False,fixture_classes_in_runtime=False,
  scope='Immutable118 historical regression plus actual119 numerical and Java/native integration; no Android device speed or quality claim.')
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');p.add_argument('--baseline-report');a=p.parse_args();r=test(a.source,a.work,a.jdk,a.ndk,a.baseline_report);print(json.dumps({'status':r['status'],'assertions':r['assertions'],'groups':list(r['tests'])}))
