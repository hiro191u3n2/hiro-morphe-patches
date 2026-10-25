#!/usr/bin/env python3
"""Actual full-photo candidate benchmark through two real native libraries.
The workload entry is exercised directly; no runtime certificate is granted.
Recorded CPU fill and comparison work is shared; this is not device evidence.
Integration provides the separately tested dual-owner native build next door.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None,native_dir=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();android=Path(os.environ['ULIKE_ANDROID_JAR'])
 native=Path(native_dir).resolve() if native_dir else work.parent/'gpu_owner'
 for name in ('libulike_gpu1960.so','libulike_gpu19119.so'):assert (native/name).is_file(),'Run real gpu_owner build first: '+str(native/name)
 sys.path.insert(0,str(source));import build1990
 sources=list(dict.fromkeys(build1990.compile_inputs([]).values()))
 fixtures=[source/'tests1995/strong/StrongTransport1995Test.java',source/'tests1995/strong/GpuTransportFault1995.java',source/'tests19119/gpu_workload/StrongWorkload19119Test.java']
 tracked=sources+fixtures+[source/'native19119/strong19119.comp',source/'native19119/engine19119.c',source/'tests19119/gpu_workload/host_workload19119.py']
 pins={str(p.relative_to(source)):sha(p) for p in tracked};classes=work/'classes';classes.mkdir(exist_ok=True)
 env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(args,name):
  p=subprocess.run(list(map(str,args)),text=True,capture_output=True,timeout=240,env=env);output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
  assert p.returncode==0 and 'WARNING in native method' not in output and 'FATAL ERROR' not in output,name+': '+output[-5000:]
  return output
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,*fixtures],'all-current-java')
 run(['cc','-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-shared','-Wl,--no-undefined','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',work/'libulike_smooth1958.so'],'real-cpu-native')
 output=run([jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx1g','-Djava.library.path='+str(native)+os.pathsep+str(work),'-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.StrongWorkload19119Test'],'actual-complete-workload')
 result=json.loads(output.split('RESULT ',1)[1]);assert result['complete_workloads']==4 and result['candidate_workloads']==2 and result['compared_pixels']==49939200
 assert pins=={str(p.relative_to(source)):sha(p) for p in tracked},'Sources changed during actual foreground test'
 result.update(source_sha256=pins,native_library_sha256={n:sha(native/n) for n in ('libulike_gpu1960.so','libulike_gpu19119.so')},benchmark_scope_is_actual_full_photo=True)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--native-dir');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,native_dir=a.native_dir),indent=2))
