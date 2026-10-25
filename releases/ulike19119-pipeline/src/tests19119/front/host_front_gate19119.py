#!/usr/bin/env python3
"""Execute production front candidate JNI and Java admission/cancellation gates.

The scheduler/Android environment is a deterministic host adapter. Neither the
forced timing admission controls nor host JNI establishes phone performance.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess
ROOT=Path(__file__).resolve().parents[2]

def run(cmd,log):
    p=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.write_text(p.stdout)
    if p.returncode:raise RuntimeError(p.stdout[-6000:])
    return p.stdout

def test(source,work,jdk,ndk=None):
    global ROOT
    ROOT=Path(source).resolve();out=Path(work).resolve();out.mkdir(parents=True,exist_ok=True);src=out/'java/com/hiro/ulike';src.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
    jdk=Path(jdk).resolve();candidate=ROOT/'native19119/front'
    spec=importlib.util.spec_from_file_location('front_builder19119',candidate/'build_front19119.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
    identity,_=builder.fingerprint()
    (out/'front_identity19119.h').write_text('#define FRONT119_SOURCE_SHA256 "'+identity+'"\n')
    flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-shared','-fPIC','-pthread','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
    run(['cc',*flags,ROOT/'native1955/single_noise1955.c','-lm','-o',out/'libulike_nr1955.so'],out/'baseline-build.log')
    run(['cc',*flags,'-I'+str(out),candidate/'front19119.c','-lm','-o',out/'libulike_front19119.so'],out/'candidate-build.log')
    (src/'CpuFrontOptimized19119.java').write_bytes((ROOT/'CpuFrontOptimized19119.java').read_bytes())
    (src/'Support.java').write_text(r'''package com.hiro.ulike;
import java.util.*;
final class ProcessingTiming1947 {
 static long epoch=1;static int adopted;static final class Trace {} static Trace current(){return new Trace();}
 static long captureEpoch1953(){return epoch;}
 public static void noiseOptimization19119(Trace t,int kind,long strips,long pixels){adopted+=(int)strips;}
}
final class GpuNoise1960 {static boolean fits=true;static boolean workspaceFits(long bytes){return fits&&bytes>0;}}
final class GpuQualification1961 {
 interface Probe {void run(Cancellation c);void close();} interface Cancellation {boolean cancelled();}
 static class Record {int variant;long cpuNanos,gpuNanos;Record(long cpu,long gpu,int variant){this.cpuNanos=cpu;this.gpuNanos=gpu;this.variant=variant;}}
 static Map<String,Record> records=new HashMap<>();static Probe pending;static boolean bg,permit=true,current=true,cancelled;static int qualified,rejected,slow,reservations,closed,committed;
 static boolean background(){return bg;}static Record restore(String key){return records.get(key);}
 static class Reservation1984 implements AutoCloseable {
  boolean accepted(){return permit;}boolean begin1985(){return permit&&current;}boolean current(){return current;}
  void commit(Probe p){pending=p;committed++;}public void close(){closed++;}
 }
 static Reservation1984 reserve1985(String key,long bytes){reservations++;return new Reservation1984();}
 static void qualified(String key,long cpu,long gpu,int variant){qualified++;records.put(key,new Record(cpu,gpu,variant));}
 static void rejectExact(String key){rejected++;records.remove(key);}
 static void rejectMeasuredSpeed1992(String key,String why,long cpu,long gpu){slow++;}
}
final class SingleNoise1955 {
 static {System.loadLibrary("ulike_nr1955");}
 static native boolean processNative(int[] i,int[] o,int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,int mw,int mh,int columns,int mr,int pw,int ph,float[] model,int[] policy);
 static final class Workspace {
  long nativeHandle;int width,rows;boolean inUse;static int drops,closes,creates;
  void acquire(){if(inUse)throw new AssertionError();inUse=true;}void relinquish(){inUse=false;}
  long handle(int w,int r){if(nativeHandle==0){nativeHandle=CpuSingle1978.createWorkspaceNative(w,r);width=w;rows=r;creates++;}return nativeHandle;}
  void dropNative(){if(nativeHandle!=0){CpuSingle1978.releaseWorkspaceNative(nativeHandle);nativeHandle=0;drops++;}}
  void close(){if(inUse)throw new AssertionError();dropNative();closes++;}
 }
}
final class CpuSingle1978 {
 static int baselineCalls,delay,mutate;static boolean cancelAfter;
 static long bytes(int w,int r){return (long)w*(4L*r+16L*Math.min(64,r))+128;}
 static native long createWorkspaceNative(int w,int r);static native void releaseWorkspaceNative(long h);static native boolean beginWorkspaceNative(long h);static native void endWorkspaceNative(long h);
 static boolean originalNativeCall19119(int[] i,int[] o,int[] u,float[] m,int[] p,SingleNoise1955.Workspace w,boolean required){
  baselineCalls++;long h=w==null?0:w.handle(u[0],u[3]-u[2]);boolean active=h!=0&&beginWorkspaceNative(h);
  try {if(required&&!active)return false;if(delay>0)try{Thread.sleep(delay);}catch(InterruptedException ex){Thread.currentThread().interrupt();}
   boolean ok=SingleNoise1955.processNative(i,o,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[7],u[8]!=0,u[9],u[10],u[11],u[12],u[13],u[14],m,p);
   if(ok&&mutate!=0)o[u[0]*u[2]]^=mutate;if(cancelAfter)GpuQualification1961.cancelled=true;return ok;
  }finally{if(active)endWorkspaceNative(h);}
 }
}
''')
    (src/'FrontGate19119Test.java').write_text(r'''package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.CancellationException;
public final class FrontGate19119Test {
 static int assertions;static void require(boolean x,String s){assertions++;if(!x)throw new AssertionError(s);}
 static void reset(){GpuQualification1961.records.clear();GpuQualification1961.pending=null;GpuQualification1961.bg=false;GpuQualification1961.permit=true;GpuQualification1961.current=true;GpuQualification1961.cancelled=false;GpuQualification1961.qualified=GpuQualification1961.rejected=GpuQualification1961.slow=0;GpuNoise1960.fits=true;CpuSingle1978.delay=0;CpuSingle1978.mutate=0;CpuSingle1978.cancelAfter=false;ProcessingTiming1947.epoch++;}
 static int[] geometry(){return new int[]{32,32,0,32,0,32,0,4,1,32,32,1,1,32,32};}
 static int[] input(){int[] r=new int[1024];for(int i=0;i<r.length;i++)r[i]=0xff000000|((i*1103515245+12345)&0xffffff);return r;}
 static float[] model(){float[] r=new float[91];Arrays.fill(r,1);r[0]=r[1]=8;r[2]=128;Arrays.fill(r,3,19,8);return r;}
 static int[] policy(){int[] r=new int[2048];for(int i=0;i<1024;i++)r[i*2]=i%32<16?0:256;return r;}
 static GpuQualification1961.Probe offer(int[] i,int[] u,float[] m,int[] p,boolean reused){CpuFrontOptimized19119.offer(i,u,m,p,reused);return GpuQualification1961.pending;}
 static void runProof(GpuQualification1961.Probe p){p.run(()->GpuQualification1961.cancelled);}
 public static void main(String[] args)throws Exception{
  int[] i=input(),u=geometry(),p=policy();float[] m=model();int[] source=i.clone(),mask=p.clone();float[] evidence=m.clone();
  reset();require(CpuFrontOptimized19119.coverage(u,p)==2,"coverage half");require(CpuFrontOptimized19119.coverage(u,null)==-1,"absent policy");int[] active=new int[2048];for(int j=0;j<1024;j++)active[j*2]=256;require(CpuFrontOptimized19119.coverage(u,active)==-1,"active policy");
  require(!CpuFrontOptimized19119.tryProcess(i,new int[1024],u,m,p,null),"no inherited proof");GpuQualification1961.records.put("cpu-single1978-workspace-exact2-time5-v1",new GpuQualification1961.Record(100,1,1));require(!CpuFrontOptimized19119.tryProcess(i,new int[1024],u,m,p,null),"old proof rejected");
  GpuQualification1961.Probe proof=offer(i,u,m,p,false);require(proof!=null,"bounded owned proof offered");int reserved=GpuQualification1961.reservations;offer(i,u,m,p,false);require(reserved==GpuQualification1961.reservations,"one snapshot per capture");
  CpuSingle1978.delay=12;runProof(proof);require(GpuQualification1961.qualified==1,"actual JNI exact proof can admit with controlled old delay");require(CpuSingle1978.baselineCalls==3,"warmup plus two complete CPU references");
  String key=GpuQualification1961.records.keySet().stream().filter(s->s.startsWith("front19119-")).findFirst().get();require(key.contains(System.getProperty("front119.expectedIdentity")),"independent implementation key");
  int[] expected=new int[1024],actual=new int[1024];CpuSingle1978.delay=0;require(CpuSingle1978.originalNativeCall19119(i,expected,u,m,p,null,false),"old JNI succeeds");require(CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"new JNI route admitted");require(Arrays.equals(expected,actual),"adopted entire output exact");require(Arrays.equals(source,i)&&Arrays.equals(mask,p)&&Arrays.equals(evidence,m),"caller input immutable");
  GpuQualification1961.records.put(key,new GpuQualification1961.Record(100,96,1));require(!CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"slower proof refused");GpuQualification1961.records.put(key,new GpuQualification1961.Record(100,95,1));require(CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"5 percent boundary allowed");GpuQualification1961.records.put(key,new GpuQualification1961.Record(100,95,2));require(!CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"wrong proof variant refused");
  proof.close();
  reset();GpuQualification1961.permit=false;require(offer(i,u,m,p,false)==null,"reservation refusal");reset();GpuNoise1960.fits=false;require(offer(i,u,m,p,false)==null,"memory refusal");reset();GpuQualification1961.current=false;require(offer(i,u,m,p,false)==null,"stale capture before copying");
  reset();GpuQualification1961.bg=true;require(offer(i,u,m,p,false)==null,"background cannot enqueue");require(!CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"background never auto selects");
  reset();proof=offer(i,u,m,p,true);require(proof!=null,"reuse baseline proof");CpuSingle1978.delay=12;int creates=SingleNoise1955.Workspace.creates;runProof(proof);require(GpuQualification1961.qualified==1,"warm old baseline passes real exact corpus");require(SingleNoise1955.Workspace.creates==creates+1,"reference scratch retained across candidate trials");require(SingleNoise1955.Workspace.closes>0,"proof scratch closed");proof.close();
  reset();proof=offer(i,u,m,p,false);CpuSingle1978.mutate=1;runProof(proof);require(GpuQualification1961.rejected==1&&GpuQualification1961.qualified==0,"one wrong pixel detected");proof.close();
  reset();proof=offer(i,u,m,p,true);GpuQualification1961.cancelled=true;boolean cancelled=false;try{runProof(proof);}catch(CancellationException expectedCancel){cancelled=true;}require(cancelled&&GpuQualification1961.qualified==0,"cancel before proof cannot certify");proof.close();
  reset();proof=offer(i,u,m,p,true);CpuSingle1978.cancelAfter=true;cancelled=false;try{runProof(proof);}catch(CancellationException expectedCancel){cancelled=true;}require(cancelled&&GpuQualification1961.qualified==0,"cancel during reference cannot certify");proof.close();
  reset();proof=offer(i,u,m,p,false);CpuSingle1978.delay=12;runProof(proof);int[] untouched=new int[1024];Arrays.fill(untouched,0x12345678);float[] bad=m.clone();bad[0]=Float.NaN;require(!CpuFrontOptimized19119.tryProcess(i,untouched,u,bad,p,null),"native refusal remains fallback");for(int x:untouched)require(x==0x12345678,"failed native transaction does not publish pixels");proof.close();
  Thread.currentThread().interrupt();require(!CpuFrontOptimized19119.tryProcess(i,actual,u,m,p,null),"interrupted caller refused");Thread.interrupted();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"native_jni_executed\":true,\"scheduler_is_host_adapter\":true,\"speed_is_controlled_gate_test_not_benchmark\":true,\"device_tested\":false}");
 }
}
''')
    run([jdk/'bin/javac','-d',classes,*src.glob('*.java')],out/'javac.log')
    result=json.loads(run([jdk/'bin/java','-Djava.library.path='+str(out),'-Dfront119.expectedIdentity='+identity,'-cp',classes,'com.hiro.ulike.FrontGate19119Test'],out/'run.log'))
    # The production caller preserves its exact old implementation after the
    # optional guard; this inverse checks actual selected release source.
    current=(ROOT/'CpuSingle1978.java').read_text();baseline=(ROOT.parents[1]/'ulike19118-pipeline/src/CpuSingle1978.java').read_text()
    marker='''        if(!required&&CpuFrontOptimized19119.tryProcess(input,output,u,model,policy,workspace))return true;
        boolean completed=originalNativeCall19119(input,output,u,model,policy,workspace,required);
        if(completed&&!required)CpuFrontOptimized19119.offer(input,u,model,policy,workspace!=null);
        return completed;
    }
    /** Immutable old native route, also the independent optional-candidate oracle. */
    static boolean originalNativeCall19119(int[] input,int[] output,int[] u,float[] model,int[] policy,SingleNoise1955.Workspace workspace,boolean required) {
'''
    if current.count(marker)!=1 or current.replace(marker,'')!=baseline:raise AssertionError('Production front hook changes old native body')
    result.update({'production_hook_inverse_verified':True,'implementation_sha256':identity,'candidate_java_sha256':hashlib.sha256((ROOT/'CpuFrontOptimized19119.java').read_bytes()).hexdigest(),'candidate_wrapper_sha256':hashlib.sha256((candidate/'front19119.c').read_bytes()).hexdigest(),'candidate_core_sha256':hashlib.sha256((candidate/'front_core19119.c').read_bytes()).hexdigest(),'positive_controls':10,'negative_controls':16,'positive_cases':['coverage class','bounded owned snapshot','one snapshot per capture','real JNI exact warmup and two trials','independent implementation key','original JNI success','candidate output exact','caller inputs unchanged','five-percent boundary','reference scratch retained and closed'],'negative_cases':['missing policy','active policy','no proof','old proof','slower proof','wrong variant','reservation refusal','memory refusal','stale capture','background offer','background selection','wrong output pixel','cancel before','cancel during','native refusal transactional','interrupted caller']})
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--jdk',required=True,type=Path);p.add_argument('--out',required=True,type=Path);p.add_argument('--source',default=ROOT,type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.out,a.jdk)))
