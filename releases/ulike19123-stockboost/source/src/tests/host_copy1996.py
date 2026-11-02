#!/usr/bin/env python3
"""No speculative CPU copies on capture; real JNI outputs and owned-probe gates.

Production Java and native arithmetic are compiler inputs. Explicit test-owned
snapshots exercise retained verifiers by reflection; no production test switch
or foreground qualification path is introduced. Native delays/counters affect
only admission tests, never represent Android performance.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess

PEERS=r'''package com.hiro.ulike;
import java.util.concurrent.*;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}static long captureId1989(long e){return e==epoch?1:0;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] tasks){for(Runnable task:tasks)task.run();}}
final class GpuNoise1960 {
 static final long MAX_BYTES=256L*1024*1024;static volatile boolean fit=true;static int fitCalls;
 static String fingerprint(){return "copy96-owned-input-test";}static boolean sessionBusy(){return false;}
 static boolean workspaceFits(long n){fitCalls++;return fit&&n>=0&&n<512L*1024*1024;}
}
final class NativeMoire1951 {
 static int mode,calls;static CountDownLatch entered,release;
 static boolean proof1976(CpuFinishCache1976.Snapshot p,int[] output,boolean cached){
  calls++;
  if(mode==3){entered.countDown();try{release.await();}catch(InterruptedException stop){Thread.currentThread().interrupt();return false;}}
  if(mode==4)return false;
  try{Thread.sleep((mode==1?cached:!cached)?12:1);}catch(InterruptedException stop){Thread.currentThread().interrupt();return false;}
  for(int y=p.first;y<p.last;y++)for(int x=0;x<p.width;x++)output[y*p.width+x]=p.source[y*p.width+x]^7;
  if(mode==2&&cached)output[p.last*p.width-1]^=1;
  return true;
 }
}
'''

HARNESS=r'''package com.hiro.ulike;
import android.content.Context;import java.nio.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
public final class Copy1996Test {
 static {System.loadLibrary("copy1996");}static int checks,factories,tag;static native void mode(int n);static native long calls(boolean cached);
 static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
 static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static void reset()throws Exception{SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();drain();GpuNoise1960.fit=true;mode(0);calls(false);calls(true);NativeMoire1951.mode=NativeMoire1951.calls=0;}
 static void drain()throws Exception{long stop=System.nanoTime()+8000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>stop)throw new AssertionError("owned proof did not drain");Thread.sleep(2);}check(true,"owned queue drained");}
 static void age()throws Exception{SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
 static String key(){return "cpu-single1978-copy96-"+(++tag);}
 static final class Work implements CpuExact1978.Work {
  final int mode;int oldCalls,newCalls,closes;final int[] shared=new int[97],old=new int[97],cached=new int[97];
  final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  Work(int m){mode=m;}
  public Object run(boolean reused){if(reused)newCalls++;else oldCalls++;
   if(mode==4)return null;if(mode==6)throw new OutOfMemoryError("controlled optional allocation");
   if(mode==5){entered.countDown();try{release.await();}catch(InterruptedException stop){Thread.currentThread().interrupt();return null;}}
   try{Thread.sleep((mode==1?reused:!reused)?12:1);}catch(InterruptedException stop){Thread.currentThread().interrupt();return null;}
   int[] result=mode==7?shared:mode==8?(reused?cached:old):new int[97];Arrays.fill(result,0xff123456);
   if(mode==2&&reused)result[96]^=1;
   if(mode==3||mode==8)result[96]=reused?newCalls:oldCalls;
   if(mode==7)result[96]=newCalls+oldCalls;
   return new Object[]{result,new float[]{Float.intBitsToFloat(0x7fc00001),-0f}};
  }
  public void close(){closes++;}
 }
 static GpuQualification1961.Probe exact(String key,Work work)throws Exception{
  Constructor<?> c=Class.forName("com.hiro.ulike.CpuExact1978$Probe").getDeclaredConstructor(String.class,long.class,CpuExact1978.Work.class);c.setAccessible(true);
  return (GpuQualification1961.Probe)c.newInstance(key,1048576L,work);
 }
 static void queue(String key,GpuQualification1961.Probe p,long bytes){check(GpuQualification1961.schedule(key,bytes,p),"explicit test-owned input queued");}
 static void foreground()throws Exception{
  reset();factories=0;int before=GpuNoise1960.fitCalls;
  CpuExact1978.Factory factory=()->{factories++;throw new AssertionError("foreground created a detached CPU candidate");};
  for(int shot=0;shot<20;shot++){
   String k=key();check(!CpuExact1978.canCapture(k,4096,1048576),"no model/probe patch collection before factory");CpuExact1978.offer(k,4096,1048576,factory);
   check(CpuFinishCache1976.capture(k,new int[320],8,40,3,37,true,true,true,128,512,16,true,true)==null,"no CPU finish source/policy capture");
   check(GpuQualification1961.retainedBytes()==0,"CPU snapshot refusal consumes no queue budget");ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();
  }
  check(factories==0,"zero factory executions across repeated captures");check(GpuNoise1960.fitCalls==before,"no proof workspace reservation or trimming on normal capture");
  check(!CpuExact1978.canCapture("invalid",Long.MAX_VALUE,Long.MAX_VALUE),"overflowed speculative request remains closed");
  GpuNoise1960.fit=false;Thread.currentThread().interrupt();try{CpuExact1978.offer("cancelled",4096,1048576,factory);check(!CpuExact1978.canCapture("cancelled",4096,1048576),"interrupted capture never captures");}finally{Thread.interrupted();}
  check(factories==0&&GpuQualification1961.retainedBytes()==0,"cancel and low memory cannot create hidden owners");GpuNoise1960.fit=true;
  String retained=key();GpuQualification1961.qualified(retained,100,80,1);check(CpuExact1978.enabled(retained)&&CpuFinishCache1976.enabled(retained),"valid exact speed certificate remains selected");
  CpuExact1978.offer(retained,4096,1048576,factory);check(CpuExact1978.enabled(retained),"capture suppression does not invalidate valid certificate");
  String slow=key();GpuQualification1961.qualified(slow,100,96,1);check(!CpuExact1978.enabled(slow)&&!CpuFinishCache1976.enabled(slow),"no bypass of five-percent gate");
  GpuQualification1961.rejectExact(retained);check(!CpuExact1978.enabled(retained)&&!CpuFinishCache1976.enabled(retained),"quality rejection still overrides old certificate");
  String stage="single-stage1981-current-cpu-exact2-time5-v2:copy96-independent";
  check(CpuExact1978.canCapture(stage,4096,1048576),"shared helper still admits GPU stage proof");
  GpuNoise1960.fit=false;check(!CpuExact1978.canCapture(stage,4096,1048576),"GPU stage peak memory gate retained");GpuNoise1960.fit=true;
  check(!CpuExact1978.canCapture(stage,96L*1024*1024+1,0),"GPU stage retained cap enforced");
  GpuQualification1961.rejectExact(stage);check(!CpuExact1978.canCapture(stage,4096,1048576),"GPU stage quality rejection retained");
  String gpu="strong-test:3:copy96-independent";final int[] state={0,0};GpuQualification1961.Probe p=new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){state[0]++;}public void close(){state[1]++;}};
  queue(gpu,p,4096);check(GpuQualification1961.retainedBytes()==4096,"GPU proof queue is still available");GpuQualification1961.captureChanged();check(state[0]==0&&state[1]==1&&GpuQualification1961.retainedBytes()==0,"GPU cancel and ownership unchanged");
 }
 static void exactGates()throws Exception{
  for(int m:new int[]{0,1,2,3,4,6,7,8}){
   reset();String k=key();Work w=new Work(m);queue(k,exact(k,w),4096);age();drain();
   check(w.closes==1,"CPU owned probe closes once mode "+m);
   if(m==0){check(w.oldCalls==3&&w.newCalls==3,"warmup and two measured full pairs retained");check(CpuExact1978.enabled(k),"exact faster CPU work admitted");}
   else check(!CpuExact1978.enabled(k),"nonqualified candidate cannot become enabled mode "+m);
   check(GpuQualification1961.exactRejected(k)==(m==2||m==3||m==7||m==8),"mismatch and instability are distinct from unavailability mode "+m);
  }
  reset();String k=key();Work w=new Work(0);queue(k,exact(k,w),4096);GpuNoise1960.fit=false;age();drain();check(w.oldCalls+w.newCalls==0&&w.closes==1&&!CpuExact1978.enabled(k),"owned input still rechecks peak budget before execution");
  reset();k=key();w=new Work(5);queue(k,exact(k,w),4096);age();check(w.entered.await(3,TimeUnit.SECONDS),"CPU owned proof starts");ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();w.release.countDown();drain();check(w.closes==1&&!CpuExact1978.enabled(k)&&!GpuQualification1961.exactRejected(k),"capture cancellation neither certifies nor poisons quality record");
  check(CpuExact1978.same(new float[]{Float.intBitsToFloat(0x7fc00001)},new float[]{Float.intBitsToFloat(0x7fc00001)}),"raw NaN payload exactness retained");
  check(!CpuExact1978.same(new float[]{0f},new float[]{-0f}),"signed zero mismatch retained");
 }
 static CpuFinishCache1976.Snapshot finish(String k)throws Exception{
  int[] source=new int[320];Arrays.fill(source,33);GpuQualification1961.Reservation1984 reservation=GpuQualification1961.reserve1985(k,8192);
  check(reservation.accepted()&&reservation.begin1985(),"owned finish input reserves before allocation");
  CpuFinishCache1976.Snapshot p=new CpuFinishCache1976.Snapshot(k,source,8,40,3,37,true,true,true,128,512,16,true,true,3,8192,1048576);
  p.reservation1993=reservation;
  for(int y=3;y<37;){int end=Math.min(37,(y+16)&~15);int[] policy=new int[(end-y)*8*4];Arrays.fill(policy,7);p.record(y,end,policy);policy[0]=999;check(p.policies[p.recorded-1][0]==7,"owned finish policy is detached");y=end;}
  return p;
 }
 static void finishGates()throws Exception{
  for(int m:new int[]{0,1,2,4}){reset();String k=key();CpuFinishCache1976.Snapshot p=finish(k);NativeMoire1951.mode=m;p.queue();age();drain();check(CpuFinishCache1976.enabled(k)==(m==0),"owned finish exact and speed gate mode "+m);check(GpuQualification1961.exactRejected(k)==(m==2),"finish failure classification mode "+m);check(p.source==null&&p.policies==null,"finish owner closed mode "+m);}
  reset();String k=key();CpuFinishCache1976.Snapshot p=finish(k);p.queue();GpuNoise1960.fit=false;age();drain();check(NativeMoire1951.calls==0&&!CpuFinishCache1976.enabled(k),"finish execution budget still enforced");
  reset();k=key();p=finish(k);NativeMoire1951.mode=3;NativeMoire1951.entered=new CountDownLatch(1);NativeMoire1951.release=new CountDownLatch(1);p.queue();age();check(NativeMoire1951.entered.await(3,TimeUnit.SECONDS),"finish owned proof starts");ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();NativeMoire1951.release.countDown();drain();check(!CpuFinishCache1976.enabled(k)&&p.source==null&&GpuQualification1961.retainedBytes()==0,"finish cancellation releases reservations without certificate");
 }
 static final int W=33,H=43;
 static int[] pixels(){int[] a=new int[W*H];Random random=new Random(1996);for(int i=0;i<a.length;i++)a[i]=0xff000000|random.nextInt(0x1000000);return a;}
 static float[] model(){float[] a=new float[91];Arrays.fill(a,1.6f);a[0]=6;a[1]=9;a[2]=114;return a;}
 static int[] geometry(int first,int last){return new int[]{W,H,first,last,0,H,0,4,1,W,H,1,1,W,H};}
 static String colourKey(int kind,int[] u)throws Exception{Method m=ColourCache1976.class.getDeclaredMethod("key",int.class,int[].class,boolean.class);m.setAccessible(true);return (String)m.invoke(null,kind,u,false);}
 static int recordBytes(int first,int last){return ((W+3)/4+1)*((last-Math.floorDiv(first-7,4)*4+3)/4)*248*4;}
 static ByteBuffer buffer(int n){return ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder());}
 static ByteBuffer cache(){return buffer(80+91*4+((W+3)/4+1)*4*1260);}
 static byte[] bytes(ByteBuffer b){ByteBuffer a=b.duplicate();a.clear();byte[] out=new byte[a.remaining()];a.get(out);return out;}
 static boolean cpu(int[] source,int[] output,float[] m){return ColourCache1976.cpu(source,output,W,H,0,H,0,H,0,4,true,W,H,1,1,W,H,m,null);}
 static float[] prepare(int[] source,float[] m){return ColourCache1976.prepare(source,W,H,0,H,0,H,0,4,true,H,1,1,W,H,m);}
 static boolean direct(ByteBuffer state,ByteBuffer out,int[] source,float[] m,int first,int last){return ColourCache1976.direct(state,out,source,W,H,first,last,0,H,0,4,true,H,1,1,W,H,m);}
 static GpuQualification1961.Probe colour(String k,int kind,int[] u,ByteBuffer tail)throws Exception{
  Constructor<?> c=Class.forName("com.hiro.ulike.ColourCache1976$Proof").getDeclaredConstructor(String.class,int.class,int[].class,int[].class,float[].class,int[].class,ByteBuffer.class);c.setAccessible(true);
  return (GpuQualification1961.Probe)c.newInstance(k,kind,pixels(),u.clone(),model(),null,tail);
 }
 static void colourGates()throws Exception{
  reset();int[] source=pixels(),expected=new int[source.length],actual=new int[source.length];float[] m=model();int[] before=source.clone();float[] beforeModel=m.clone();
  check(cpu(source,expected,m),"original CPU colour route succeeds");check(calls(false)==1&&calls(true)==0,"unknown CPU uses original loader");
  check(Arrays.equals(source,before)&&Arrays.equals(m,beforeModel),"source and model unchanged");check(GpuQualification1961.retainedBytes()==0,"CPU colour route creates no snapshot");
  String ck=colourKey(0,geometry(0,H));GpuQualification1961.qualified(ck,100,80,1);check(cpu(source,actual,m),"certified CPU route succeeds");check(calls(true)==1&&calls(false)==0,"existing CPU certificate used");check(Arrays.equals(expected,actual),"real JNI certified and default ARGB identical");
  GpuQualification1961.rejectExact(ck);cpu(source,actual,m);check(calls(false)==1&&calls(true)==0&&Arrays.equals(expected,actual),"rejected CPU route returns to exact default");
  float[] expectedRecords=prepare(source,m);check(expectedRecords!=null,"default residual records succeed");check(calls(false)==1&&calls(true)==0,"unknown preparation uses original loader");
  String pk=colourKey(1,geometry(0,H));GpuQualification1961.qualified(pk,100,80,1);float[] actualRecords=prepare(source,m);check(calls(true)==1&&calls(false)==0,"existing preparation certificate used");check(CpuExact1978.same(expectedRecords,actualRecords),"real JNI residual records raw-float exact");
  for(int[] band:new int[][]{{0,23},{23,H}}){ByteBuffer c1=cache(),c2=cache();if(band[0]>0){check(direct(c1,buffer(recordBytes(0,23)),source,m,0,23),"warm original cache");check(direct(c2,buffer(recordBytes(0,23)),source,m,0,23),"warm certified cache");}
   ByteBuffer r1=buffer(recordBytes(band[0],band[1])),r2=buffer(r1.capacity());check(direct(c1,r1,source,m,band[0],band[1]),"default direct native succeeds");String dk=colourKey(2,geometry(band[0],band[1]));GpuQualification1961.qualified(dk,100,80,1);calls(true);calls(false);check(direct(c2,r2,source,m,band[0],band[1]),"certified direct native succeeds");check(calls(true)==1&&calls(false)==0,"existing direct certificate used");check(Arrays.equals(bytes(r1),bytes(r2))&&Arrays.equals(bytes(c1),bytes(c2)),"real JNI direct records and transactional tail exact");
  }
  check(GpuQualification1961.retainedBytes()==0,"all CPU/preparation/direct foreground routes leave zero proof ownership");
  for(int kind=0;kind<3;kind++)for(int outcome=0;outcome<2;outcome++){
   reset();String k=key();mode(outcome==0?1:2);GpuQualification1961.Probe p=colour(k,kind,geometry(0,H),kind==2?cache():null);queue(k,p,262144);age();drain();check((GpuQualification1961.restore(k)!=null)==(outcome==0),"owned JNI colour exact speed qualification kind "+kind+" outcome "+outcome);check(calls(false)==3&&calls(true)==3,"owned JNI colour warmup and two pairs kind "+kind);Field f=p.getClass().getDeclaredField("input");f.setAccessible(true);check(f.get(p)==null,"owned colour input released");
  }
  reset();String k=key();mode(3);queue(k,colour(k,0,geometry(0,H),null),262144);age();drain();check(GpuQualification1961.exactRejected(k)&&GpuQualification1961.restore(k)==null,"one changed native pixel still permanently rejected");
  reset();k=key();GpuQualification1961.Probe p=colour(k,0,geometry(0,H),null);queue(k,p,262144);GpuQualification1961.captureChanged();check(GpuQualification1961.retainedBytes()==0&&calls(false)==0&&calls(true)==0,"cancelled colour proof never executes and releases owner");
  reset();k=key();queue(k,colour(k,0,geometry(0,H),null),262144);GpuNoise1960.fit=false;age();drain();check(calls(false)==0&&calls(true)==0&&GpuQualification1961.restore(k)==null,"colour peak memory still checked before JNI");
  reset();mode(4);check(!cpu(source,actual,m),"native refusal remains false for caller fallback");check(GpuQualification1961.retainedBytes()==0,"native failure creates no retry snapshot");
 }
 public static void main(String[] args)throws Exception{
  GpuQualification1961.initialize(new Context());foreground();exactGates();finishGates();colourGates();reset();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"foreground_cpu_copy_disabled\":true,\"existing_certificates_and_original_fallback_verified\":true,\"explicit_owned_verifiers_exact_speed_cancel_memory_verified\":true,\"reused_result_alias_regression_verified\":true,\"actual_colour_jni_outputs_compared\":true,\"gpu_queue_independent\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
'''

def mod(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    tracked=[source/n for n in ['CpuExact1978.java','ColourCache1976.java','CpuFinishCache1976.java','GpuQualification1961.java','native1955/single_noise1955.c','native1960/residual_blocks1961.c','tests/host_copy1996.py']]
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    def run(name,args):
        done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=150);(work/(name+'.log')).write_text(done.stdout+done.stderr)
        if done.returncode or 'WARNING in native method' in done.stdout+done.stderr:raise AssertionError(name+'\n'+(done.stdout+done.stderr)[-14000:])
        return done.stdout
    fixtures=mod('copy96_android',source/'host_qualification1981.py').FIXTURES;paths=[]
    for name,body in fixtures.items():
        if not name.startswith('android/'):continue
        path=work/'generated'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);paths.append(path)
    for name,body in [('CopyPeers1996.java',PEERS),('Copy1996Test.java',HARNESS)]:
        path=work/'generated'/name;path.write_text(body);paths.append(path)
    single=(source/'native1955/single_noise1955.c').read_text()
    residual='static int export_block1961'+(source/'native1960/residual_blocks1961.c').read_text().split('static int export_block1961',1)[1]
    native=single+'\n'+residual
    needle='int previous=colour_enabled1976;colour_enabled1976=cached?1:0;'
    if native.count(needle)!=3:raise AssertionError('Expected exact three scoped JNI wrappers')
    native=native.replace(needle,'copy96_gate(cached);'+needle)
    cpu='''    jboolean result=Java_com_hiro_ulike_SingleNoise1955_processNative(env,cls,'''
    if native.count(cpu)!=1:raise AssertionError('Expected one CPU wrapper')
    native=native.replace(cpu,'''    if(copy96_mode==4){colour_enabled1976=previous;return JNI_FALSE;}\n'''+cpu)
    end='    colour_enabled1976=previous;return result;'
    native=native.replace(end,'''    if(copy96_mode==3&&cached&&result){jint bad=0x12345678;(*env)->SetIntArrayRegion(env,output_array,begin*width,1,&bad);}\n'''+end,1)
    pre='''#define _POSIX_C_SOURCE 200809L
#include <jni.h>
#include <time.h>
static int copy96_mode;static long copy96_old,copy96_new;
static void copy96_gate(int cached){if(cached)copy96_new++;else copy96_old++;if((copy96_mode==1&&!cached)||(copy96_mode==2&&cached)){struct timespec delay={0,12000000};nanosleep(&delay,NULL);}}
'''
    post='''
JNIEXPORT void JNICALL Java_com_hiro_ulike_Copy1996Test_mode(JNIEnv *e,jclass c,jint mode){(void)e;(void)c;copy96_mode=mode;}
JNIEXPORT jlong JNICALL Java_com_hiro_ulike_Copy1996Test_calls(JNIEnv *e,jclass c,jboolean cached){(void)e;(void)c;long result=cached?copy96_new:copy96_old;if(cached)copy96_new=0;else copy96_old=0;return result;}
'''
    path=work/'observed_native.c';path.write_text(pre+native+post);lib=work/'libcopy1996.so'
    run('native_compile',['cc','-std=c11','-O3','-shared','-fPIC','-pthread','-Wall','-Wextra','-Werror','-fno-fast-math','-ffp-contract=off','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),path,'-lm','-o',lib])
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run('java_compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*paths,*tracked[:4]])
    report=json.loads(run('runtime',[jdk/'bin/java','-ea','-Xcheck:jni','-Xmx512m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.Copy1996Test']).strip().splitlines()[-1])
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Sources changed during CPU copy policy test')
    report.update(source_sha256=pins,native_library_sha256=sha(lib),native_observation_scope='Three JNI entry counters, controlled delay, CPU refusal and one-pixel corruption only; actual native arithmetic unchanged.')
    (work/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');return report

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');args=p.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),ensure_ascii=False))
