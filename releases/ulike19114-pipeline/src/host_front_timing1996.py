#!/usr/bin/env python3
"""Real production front CPU/Java control flow and Java pixel fallback.
Transport peers inject success/false/symbol failure; ABI mismatch and missing
library use actual JVM loader. Existing native math suites independently test
actual C output. This suite makes no device-speed or native-math assertion.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess
ROOT=Path(__file__).resolve().parent
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['JAVA_HOME']);classes=work/'classes';classes.mkdir(exist_ok=True)
 paths=[source/'SingleNoise1955.java',source/'tests1996/front/FrontNativePeers1996.java',source/'tests1996/front/FrontNative1996Test.java']
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths};commands=[]
 def run(cmd,name):
  commands.append(list(map(str,cmd)));p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=90);text=p.stdout+p.stderr;(work/(name+'.log')).write_text(text)
  if p.returncode:raise AssertionError(name+'\n'+text)
  return text
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*paths],'compile')
 native=work/'abi';native.mkdir(exist_ok=True);c=native/'abi.c';c.write_text('#include <jni.h>\nJNIEXPORT jint JNICALL Java_com_hiro_ulike_SingleNoise1955_nativeAbi(JNIEnv*e,jclass c){return 1954;}\n')
 run(['gcc','-shared','-fPIC','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),c,'-o',native/'libulike_nr1955.so'],'abi-library')
 cases={}
 for name,arg,lib in [('fallbacks',[],work/'absent'),('missing',['missing'],work/'absent'),('abi',['abi'],native)]:
  out=run([jdk/'bin/java','-ea','-Xcheck:jni','-Djava.library.path='+str(lib),'-cp',classes,'com.hiro.ulike.FrontNative1996Test',*arg],name)
  cases[name]=json.loads(re.search(r'^RESULT (.*)$',out,re.M).group(1))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}:raise AssertionError('Sources changed during frontend control-flow proof')
 result=dict(status='passed',assertions=sum(c['assertions'] for c in cases.values()),cases=cases,production_source_sha256=pins,controlled_native_transport=True,actual_jvm_loading=True,physical_android_tested=False,device_speedup_verified=False)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
