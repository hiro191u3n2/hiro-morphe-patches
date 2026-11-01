#!/usr/bin/env python3
"""Current .119 Java integration, baseline-native regression, and optional gates."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);sys.modules[name]=m;spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['JAVA_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 os.environ['ULIKE_JDK_HOME']=str(jdk);os.environ['ULIKE_NDK_HOME']=str(ndk)
 os.environ['PATH']=str(jdk/'bin')+os.pathsep+os.environ.get('PATH','');os.environ['EGL_PLATFORM']='surfaceless';os.environ['LIBGL_ALWAYS_SOFTWARE']='1'
 sys.path.insert(0,str(source));import build19101
 pins=build19101.source_pins();tests={}
 groups=[('strong_transport','host_strong_transport1995.py'),('model_parallel','host_model_parallel1996.py'),('strong_adaptive','host_strong_adaptive1998.py'),('memory','host1990_memory.py'),('front_gate','tests19119/front/host_front_gate19119.py')]
 # These suites compile actual modified Java and exercise the preserved old
 # native engine. Candidate-engine ownership/admission has separate live tests.
 for name,path in groups:
  r=module('integration119_'+name,source/path).test(source,work/name,jdk,ndk)
  assert r.get('status')=='passed' and type(r.get('assertions')) is int and r['assertions']>0
  tests[name]=r;print('PASS current119 '+name+' '+str(r['assertions']),flush=True)
 classes=work/'strong_transport/standard/classes';fixture=source/'tests19119/integration/NoiseTiming19119Test.java'
 cp=str(classes)+os.pathsep+os.environ['ULIKE_ANDROID_JAR']
 commands=[[jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,fixture],[jdk/'bin/java','-cp',cp,'com.hiro.ulike.NoiseTiming19119Test']]
 for i,cmd in enumerate(commands):
  p=subprocess.run(list(map(str,cmd)),text=True,capture_output=True,timeout=60);(work/('timing-'+str(i)+'.log')).write_text(p.stdout+p.stderr);assert p.returncode==0,p.stdout+p.stderr
 tests['capture_timing']=json.loads(p.stdout)
 assert tests['capture_timing']['assertions']>=12
 # Test modules for actual optional engine and its qualification are supplied
 # alongside the implementation, and are mandatory (never silently skipped).
 for name,path in [('gpu_owner','tests19119/gpu/host_dual_owner19119.py'),('gpu_foreground','tests19119/gpu_foreground/host_foreground19119.py'),('gpu_workload','tests19119/gpu_workload/host_workload19119.py'),('gpu_gate','tests19119/gpu_gate/host_gate19119.py')]:
  r=module('integration119_'+name,source/path).test(source,work/name,jdk,ndk)
  assert r.get('status')=='passed' and type(r.get('assertions')) is int and r['assertions']>0
  tests[name]=r;print('PASS current119 '+name+' '+str(r['assertions']),flush=True)
 assert pins==build19101.source_pins(),'Current integration source changed during execution'
 result=dict(schema='ulike19119-noise-integration-host-v1',status='passed',assertions=sum(r['assertions'] for r in tests.values()),tests=tests,
  current_source_sha256=pins,current_production_java_executed=True,baseline_native_with_current_wrappers=True,source_stability_verified=True,
  physical_android_tested=False,device_speedup_verified=False,fixture_classes_in_runtime=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
