#!/usr/bin/env python3
"""Exercise real JNI two-bank completion and fenced phase reclamation on Mesa."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess
HARNESS=r'''
package com.hiro.ulike;
import java.util.Arrays;
public final class Native1991Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static int[] source(int seed){int[] a=new int[64*64];for(int i=0;i<a.length;i++)a[i]=0xff000000|((i*31+seed)&255)<<16|((i*7+seed)&255)<<8|((i*19+seed)&255);return a;}
 static int[] expected(int[] s){int[] out=new int[32*32];for(int y=0;y<32;y++)for(int x=0;x<32;x++){int r=0,g=0,b=0;for(int j=0;j<2;j++)for(int k=0;k<2;k++){int p=s[(y*2+j)*64+x*2+k];r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;}out[y*32+x]=0xff000000|((r+2)/4)<<16|((g+2)/4)<<8|((b+2)/4);}return out;}
 static GpuNoise1960.Batch batch(int[] a,int first){int[] u=new int[32];u[1]=64;u[2]=64;u[3]=32;u[4]=32;return new GpuNoise1960.Batch().upload(first,a).allocate(first+1,4096).dispatch(GpuNoise1960.ANALYSIS,new int[]{first,first+1,first,first,first,first},u,null,1024);}
 public static void main(String[] args){
  check(GpuNoise1960.available()&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS),"actual JNI/GLES analysis available");
  GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"session opened");
  try{
   int[] a=source(1),b=source(97);int[] slots={0,1,10,11,24};long[] sizes={16384,4096,16384,4096,16384};
   GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(slots,sizes,0);check(lease!=null,"exact native capacity lease");
   check(!s.releaseSlots1991(new int[]{0}),"live lease refuses release without mutation");
   check(s.upload(24,a),"live resident source uploaded");
   GpuNoise1960.Ticket one=s.submit(batch(a,0),0),two=s.submit(batch(b,10),1);check(one!=null&&two!=null,"two disjoint banks submitted");
   check(!s.releaseSlots1991(new int[]{24}),"pending bank protects every live phase slot");
   int[] x=new int[1028],y=new int[1028];Arrays.fill(x,37);Arrays.fill(y,59);
   check(s.collectManyInto(one,new int[]{1},new int[]{1024},new int[][]{x},new int[]{2}),"fused collectMany completes ticket");
   check(s.collectInto(two,11,1024,y,2),"fused collectInto completes other ticket");
   check(x[0]==37&&x[1]==37&&x[1026]==37&&x[1027]==37&&y[0]==59&&y[1027]==59,"private readback offsets remain bounded");
   check(Arrays.equals(Arrays.copyOfRange(x,2,1026),expected(a))&&Arrays.equals(Arrays.copyOfRange(y,2,1026),expected(b)),"both bank outputs retain exact CPU pixels");
   check(!s.collectInto(two,11,1024,y,2),"ticket cannot be consumed twice");
   lease.close();long[] before=s.capacity1976();long bytes=GpuNoise1960.retainedBytes();
   check(!s.releaseSlots1991(new int[]{0,0})&&!s.releaseSlots1991(new int[]{25}),"invalid sets refuse before release");
   check(Arrays.equals(before,s.capacity1976()),"invalid request does not mutate any native capacity");
   check(s.releaseSlots1991(new int[]{0,1,10,11}),"only obsolete completed bank slots released");
   long[] after=s.capacity1976();check(after[0]==0&&after[1]==0&&after[10]==0&&after[11]==0&&after[24]==before[24],"resident slot survives precise phase release");
   check(bytes-GpuNoise1960.retainedBytes()==before[0]+before[1]+before[10]+before[11],"retained accounting drops by actual completed capacities");
   check(Arrays.equals(s.readInts(24,a.length),a),"remaining resident bytes are unchanged");
   check(s.releaseSlots1991(new int[]{0,1}),"already released slots are harmless");
   lease=s.reserveCapacity1971(new int[]{0,1},new long[]{16384,4096},0);check(lease!=null&&s.run(batch(b,0)),"released storage can be allocated and submitted again");lease.close();
   check(s.releaseSlots1991(new int[]{0}),"non-ticket run is drained before input release");
   check(Arrays.equals(s.readInts(1,1024),expected(b)),"dependent output survives fenced source release");
   check(s.failureCode1971()==0,"refusals do not poison exact fallback or successful session");
  }finally{s.close();}
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"closed session releases ownership and reservations");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":true,\"physical_android_tested\":false}");
 }
}
'''
STUBS='''package com.hiro.ulike;
final class WholeRoute1953{static long retainedBytes(){return 0;}}
final class GpuFinish1953{static long retainedBytes(){return 0;}}
final class SpeedWorkers1935{static long nativeRetainedBytes1956(){return 0;}static long nativeRetainedBytes1989(long[] v,int o){return 0;}}
'''
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or source.parent/'tools/jdk-21.0.8+9').resolve();ndk=Path(ndk or source.parent/'tools/android-ndk-r27c').resolve()
 native=source/'native1960';classes=work/'classes';classes.mkdir(exist_ok=True)
 for name in ('EGL','GLES3','KHR'):shutil.copytree(ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'/name,work/'headers'/name,dirs_exist_ok=True)
 spec=importlib.util.spec_from_file_location('native1991_builder',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
 (work/'Native1991Test.java').write_text(HARNESS);(work/'MemoryPeers1991.java').write_text(STUBS)
 environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
 def run(command,label):
  result=subprocess.run(list(map(str,command)),capture_output=True,text=True,env=environment,timeout=90);output=result.stdout+result.stderr;(work/(label+'.log')).write_text(output)
  if result.returncode or 'WARNING in native method' in output:raise AssertionError(label+' failed: '+output[-10000:])
  return output
 run([jdk/'bin/javac','--release','8','-d',classes,source/'GpuNoise1960.java',work/'MemoryPeers1991.java',work/'Native1991Test.java'],'compile-java')
 run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',work/'libulike_gpu1960.so'],'compile-native')
 output=run([jdk/'bin/java','-Xcheck:jni','-Xmx512m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.Native1991Test'],'execute')
 result=json.loads(output.split('RESULT ',1)[1].strip());result['native_sha256']=hashlib.sha256((work/'libulike_gpu1960.so').read_bytes()).hexdigest();result['device_speedup_verified']=False
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',default=Path(__file__).parent);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
