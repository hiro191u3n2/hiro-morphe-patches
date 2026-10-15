#!/usr/bin/env python3
"""Retain every real .95 finish recovery test, then verify .98 restore text."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
 spec=importlib.util.spec_from_file_location('finish_recovery98_inherited',source/'host_finish_recovery1995.py')
 inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
 evidence=inherited.test(source,work/'inherited',jdk,ndk)
 classes=work/'restore-classes';classes.mkdir(exist_ok=True)
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 cp=os.pathsep.join(map(str,[work/'inherited/inherited/fault-classes',work/'inherited/inherited/policy/classes',android]))
 harness=source/'tests1998/timing/WideRestore1998Test.java'
 def run(argv,name):
  r=subprocess.run(list(map(str,argv)),capture_output=True,text=True,timeout=300,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
  output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+'\n'+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,harness],'restore-compile')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'inherited/inherited/policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.WideRestore1998Test'],'restore-jni')
 restored=json.loads(output.split('RESULT ',1)[1].strip())
 result=dict(evidence)
 result.update(assertions=evidence['assertions']+restored['assertions'],wide_restore=restored,restore_harness_sha256=hashlib.sha256(harness.read_bytes()).hexdigest(),production_restore_source_sha256=hashlib.sha256((source/'GpuFinishDefault1990.java').read_bytes()).hexdigest())
 for key in ('wide_restore_status_verified','no_extra_gpu_dispatch_verified'):
  assert restored.get(key) is True,key
  result[key]=True
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('source','work'):p.add_argument('--'+name,required=True)
 p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
