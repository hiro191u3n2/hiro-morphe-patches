#!/usr/bin/env python3
"""Retain the complete standard39 oracle gate and execute optional48 proof.
Host timing controls only this ephemeral host test; no device speed is claimed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 spec=importlib.util.spec_from_file_location('startup_math94_base',source/'host_startup_math1990.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
 standard=base.test(source,work/'standard',jdk,ndk)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar')))
 harness=source/'tests1994/strong/StrongAdaptive1994Test.java';state_files=[source/'tests1994/strong/AdaptivePeers1994.java',source/'tests1994/strong/StrongAdaptiveState1994Test.java'];tracked=[source/'GpuNoise1960.java',source/'GpuStrongDefault1990.java',source/'GpuStrong1960.java',source/'GpuStartup1990.java',harness,Path(__file__),*state_files]
 pins={str(p.relative_to(source)):sha(p) for p in tracked};classes=work/'standard/classes';cp=str(classes)+os.pathsep+str(android)
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');commands=[]
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,text=True,capture_output=True,env=env,timeout=300);output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
  if p.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(name+' failed:\n'+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-cp',cp,'-d',classes,harness],'adaptive-compile')
 java=[jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m']
 oracle=run(java+['-cp',cp,'com.hiro.ulike.StrongAdaptive1994Test','oracle'],'large-java-oracle')
 native=run(java+['-Djava.library.path='+str(work/'standard/cpu-native'),'-cp',cp,'com.hiro.ulike.StrongAdaptive1994Test','native'],'large-native-oracle')
 if oracle!=native or 'ORACLE ' not in oracle:raise AssertionError('256-row Java and native CPU oracles differ')
 output=run(java+['-Djava.library.path='+str(work/'standard'),'-cp',cp,'com.hiro.ulike.StrongAdaptive1994Test'],'optional-native-proof')
 optional=json.loads(output.split('RESULT ',1)[1].strip())
 state_classes=work/'state-classes';state_classes.mkdir(exist_ok=True)
 run([jdk/'bin/javac','--release','8','-d',state_classes,source/'GpuStrongDefault1990.java',*state_files],'state-compile')
 state=json.loads(run(java+['-cp',state_classes,'com.hiro.ulike.StrongAdaptiveState1994Test'],'state-faults'))
 if state.get('status')!='passed' or state.get('assertions',0)<10:raise AssertionError('Missing controlled fallback/cancel/speed assertions')
 if optional.get('status')!='passed' or optional.get('optional_cases')!=70 or optional.get('compared_pixels',0)<1800000:raise AssertionError('Incomplete optional production proof')
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Source changed during optional proof')
 result=dict(status='passed',assertions=standard['assertions']+optional['assertions']+state['assertions'],standard=standard,optional=optional,controlled_state=state,large_java_native_bit_exact=True,actual_jni=True,physical_android_tested=False,device_speedup_verified=False,source_sha256=pins)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
