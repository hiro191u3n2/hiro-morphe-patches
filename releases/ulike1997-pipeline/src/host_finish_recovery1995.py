#!/usr/bin/env python3
"""Retain the full .93 policy/geometry/fallback audit, then execute .95 recovery."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
 spec=importlib.util.spec_from_file_location('finish_inherited1995',source/'host_finish1993.py')
 inherited=importlib.util.module_from_spec(spec);spec.loader.exec_module(inherited)
 evidence=inherited.test(source,work/'inherited',jdk,ndk)
 classes=work/'recovery-classes';classes.mkdir(exist_ok=True)
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 cp=os.pathsep.join(map(str,[work/'inherited/fault-classes',work/'inherited/policy/classes',android]))
 harness=source/'tests1995/finish/FinishRecovery1995Test.java'
 def run(argv,name):
  r=subprocess.run(list(map(str,argv)),capture_output=True,text=True,timeout=300,env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1'))
  output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
  if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+'\n'+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,harness],'recovery-compile')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'inherited/policy'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.FinishRecovery1995Test'],'recovery-jni')
 recovery=json.loads(output.split('RESULT ',1)[1].strip())
 if recovery.get('status')!='passed' or recovery.get('wide_cases')!=28 or recovery.get('wide_exact_pixels',0)<2500000 or not recovery.get('cancel_rearm_verified') or not recovery.get('bounded_metadata_retry_verified'):raise AssertionError('Incomplete .95 recovery evidence')
 result={'status':'passed','assertions':evidence['assertions']+recovery['assertions'],'resident_policy_trace_verified':evidence['resident_policy_trace_verified'],'inherited':evidence,'recovery':recovery,'physical_android_tested':False,'device_speedup_verified':False,'harness_sha256':hashlib.sha256(harness.read_bytes()).hexdigest()}
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('source','work'):p.add_argument('--'+name,required=True)
 p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
