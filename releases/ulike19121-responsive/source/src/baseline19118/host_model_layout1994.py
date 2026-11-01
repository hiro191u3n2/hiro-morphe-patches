#!/usr/bin/env python3
"""Retain full real model JNI math and verify optional measured layout admission."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(ndk.parent/'android.jar')))
 tracked=[source/p for p in ['GpuModelDefault1990.java','GpuAnalysis1961.java','host_model_layout1994.py','tests1994/model/ModelLayout1994Test.java','tests1994/model/ModelLayoutMath1994Test.java','tests1994/model/ModelFallback1994Test.java']]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(name,args):
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,env=environment,timeout=300);output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
  if p.returncode or 'WARNING in native method' in output or 'FATAL ERROR in native method' in output:raise AssertionError(name+'\n'+output[-14000:])
  return output
 controls=work/'control-classes';controls.mkdir(exist_ok=True)
 run('control-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',controls,source/'GpuModelDefault1990.java',source/'tests1994/model/ModelLayout1994Test.java'])
 controlled=json.loads(run('control-run',[jdk/'bin/java','-ea','-cp',controls,'com.hiro.ulike.ModelLayout1994Test']).split('RESULT ',1)[1])
 spec=importlib.util.spec_from_file_location('model94_retained',source/'host_model_math1990.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 retained=module.test(source,work/'retained90',jdk,ndk)
 classes=work/'retained90/classes'
 run('speed-input-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,source/'tests1994/model/ModelLayoutMath1994Test.java'])
 numerical=json.loads(run('speed-input-jni',[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'retained90')+os.pathsep+str(work/'retained90/cpu-native'),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ModelLayoutMath1994Test']).split('RESULT ',1)[1])
 if numerical.get('model_cases')!=6 or numerical.get('compared_words',0)<1800000:raise AssertionError('Full-width model math evidence incomplete')
 # Isolated copy injects failures only at optional candidate entry/return.
 # Both numerical graphs, Java selection/fallback owner and JNI are unchanged.
 body=(source/'GpuAnalysis1961.java').read_text()
 marker='return residentCandidate1990(halfSource,width,height,noise,shadows,sourceEvidence,runtime,variant,true);'
 if body.count(marker)!=1:raise AssertionError('Model optional entry anchor changed')
 body=body.replace('private GpuAnalysis1961() {}','private GpuAnalysis1961() {}\n    static int fault1994;static String tried1994="";')
 body=body.replace(marker,'''tried1994+=variant;
        if(variant==1){if(fault1994==1)return null;if(fault1994==2)throw new RuntimeException("controlled");
            if(fault1994==3)throw new LinkageError("controlled");if(fault1994==4)throw new OutOfMemoryError("controlled");
            if(fault1994==5)throw new CancellationException("controlled");}
        StrongNoise1958.Model result=residentCandidate1990(halfSource,width,height,noise,shadows,sourceEvidence,runtime,variant,true);
        if(variant==1&&fault1994==6)Thread.currentThread().interrupt();
        if(variant==1&&fault1994==7)GpuModelDefault1990.revoked=true;
        return result;''')
 adapted=work/'fault-source/GpuAnalysis1961.java';adapted.parent.mkdir(exist_ok=True);adapted.write_text(body)
 faults=work/'fault-classes';faults.mkdir(exist_ok=True)
 run('fallback-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(android),'-d',faults,adapted,source/'tests1994/model/ModelFallback1994Test.java'])
 fallback=json.loads(run('fallback-jni',[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'retained90')+os.pathsep+str(work/'retained90/cpu-native'),'-cp',str(faults)+os.pathsep+str(classes)+os.pathsep+str(android),'com.hiro.ulike.ModelFallback1994Test']).split('RESULT ',1)[1])
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Model layout source changed during proof')
 result=dict(status='passed',assertions=controlled['assertions']+retained['assertions']+numerical['assertions']+fallback['assertions'],tests=[controlled,retained,numerical,fallback],source_sha256=pins,model_cases=retained['model_cases']+numerical['model_cases'],compared_words=retained['compared_words']+numerical['compared_words'],physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
