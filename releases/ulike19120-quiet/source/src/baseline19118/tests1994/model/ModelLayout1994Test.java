package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Real layout admission owner, controlled transport/time only. Numerical
 * equality of all three layouts is checked separately through actual JNI. */
public final class ModelLayout1994Test {
 static int assertions;
 static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void baseline(){GpuCertificates1990.passed(GpuModelDefault1990.SUITE,32,100,100,100);}
 static void reset(){GpuCertificates1990.values.clear();GpuCertificates1990.rejects.clear();GpuAnalysis1961.calls=GpuAnalysis1961.closed=0;GpuAnalysis1961.fault=0;GpuNoise1960.supported=true;
  try{for(String name:new String[]{"warmedLayouts1994","unavailableLayouts1994"}){java.lang.reflect.Field f=GpuModelDefault1990.class.getDeclaredField(name);f.setAccessible(true);f.setInt(null,0);}}
  catch(Exception unavailable){throw new AssertionError(unavailable);}baseline();}
 public static void main(String[] args){
  check(!GpuModelDefault1990.faster1994(0,1)&&!GpuModelDefault1990.faster1994(100,0),"invalid time never passes");
  check(GpuModelDefault1990.faster1994(100,90)&&!GpuModelDefault1990.faster1994(100,91),"ten percent threshold exact");
  reset();check(GpuModelDefault1990.variant1994()==0,"baseline default before layout proof");
  GpuCertificates1990.passed(GpuModelDefault1990.layoutSuite1994(1),32,100,100,50);
  check(GpuModelDefault1990.variant1994()==0,"quality alone never changes route");
  GpuCertificates1990.passed(GpuModelDefault1990.speedSuite1994(1),4,100,100,50);
  check(GpuModelDefault1990.variant1994()==0,"persisted certificates cannot compile a cold layout on foreground");
  check(GpuModelDefault1990.qualifyVariant1994(1,LIVE)&&GpuAnalysis1961.calls==0,"cached layout prewarm does not rerun pixels or timing");
  check(GpuModelDefault1990.variant1994()==1,"quality plus speed selects candidate");
  GpuCertificates1990.passed(GpuModelDefault1990.layoutSuite1994(2),32,100,100,30);
  GpuCertificates1990.passed(GpuModelDefault1990.speedSuite1994(2),4,100,100,30);
  check(GpuModelDefault1990.qualifyVariant1994(2,LIVE)&&GpuAnalysis1961.calls==0,"second cached layout prewarm also runs no image work");
  check(GpuModelDefault1990.variant1994()==2,"faster measured approved candidate selected");
  GpuCertificates1990.reject(GpuModelDefault1990.layoutSuite1994(2),"one-bit");
  check(GpuModelDefault1990.variant1994()==1,"quality revocation immediately removes candidate");
  GpuCertificates1990.reject(GpuModelDefault1990.SUITE,"baseline");
  check(GpuModelDefault1990.variant1994()==0&&!GpuModelDefault1990.admitted1994(1),"baseline rejection blocks alternatives");
  reset();GpuCertificates1990.passed(GpuModelDefault1990.layoutSuite1994(1),32,100,100,50);GpuCertificates1990.passed(GpuModelDefault1990.speedSuite1994(1),4,100,100,50);
  final int[] checks={0};try{GpuModelDefault1990.qualifyVariant1994(1,new GpuQualification1961.Cancellation(){public boolean cancelled(){return ++checks[0]>=3;}});throw new AssertionError("prewarm cancel swallowed");}catch(CancellationException expected){assertions++;}
  check(GpuModelDefault1990.variant1994()==0&&GpuAnalysis1961.calls==0,"cancelled partial prewarm retains standard without image work");
  reset();GpuAnalysis1961.fault=1;
  check(!GpuModelDefault1990.qualifyVariant1994(1,LIVE),"mismatch rejected");
  check(GpuModelDefault1990.ready()&&GpuCertificates1990.rejected(GpuModelDefault1990.layoutSuite1994(1)),"optional mismatch preserves baseline");
  check(GpuAnalysis1961.calls==GpuAnalysis1961.closed,"mismatch releases model owner");
  reset();GpuNoise1960.supported=false;
  check(!GpuModelDefault1990.qualifyVariant1994(2,LIVE)&&GpuAnalysis1961.calls==0&&GpuModelDefault1990.ready(),"unsupported never dispatches or revokes baseline");
  reset();GpuAnalysis1961.fault=2;
  check(!GpuModelDefault1990.qualifyVariant1994(1,LIVE)&&GpuModelDefault1990.ready(),"OOM leaves standard ready");
  reset();GpuAnalysis1961.fault=3;
  try{GpuModelDefault1990.qualifyVariant1994(1,LIVE);throw new AssertionError("cancellation swallowed");}catch(CancellationException expected){assertions++;}
  check(GpuCertificates1990.restore(GpuModelDefault1990.layoutSuite1994(1))==null,"cancellation writes no proof");
  reset();GpuAnalysis1961.fault=4;
  check(!GpuModelDefault1990.qualifyVariant1994(1,LIVE),"slower measured candidate refused");
  check(GpuCertificates1990.restore(GpuModelDefault1990.layoutSuite1994(1))!=null&&GpuCertificates1990.restore(GpuModelDefault1990.speedSuite1994(1))==null,"exact slow candidate does not earn speed permission");
  check(GpuModelDefault1990.variant1994()==0&&GpuAnalysis1961.calls==GpuAnalysis1961.closed,"slow proof returns every owner");
  reset();check(GpuModelDefault1990.qualifyVariant1994(2,LIVE),"all exact comparisons and alternating speed trials pass");
  check(GpuAnalysis1961.calls==44&&GpuAnalysis1961.closed==44,"32 corpus plus twelve large warmup/trial outputs all compared and closed");
  check(GpuModelDefault1990.variant1994()==2,"full successful proof selects candidate");
  GpuModelDefault1990.unavailable1994(2);
  check(GpuModelDefault1990.variant1994()==0&&GpuModelDefault1990.ready(),"runtime candidate failure keeps baseline");
  reset();GpuModelDefault1990.requestOptional1994();
  check(GpuQualification1961.jobs.size()==2&&GpuQualification1961.bytes==1024&&GpuAnalysis1961.calls==0,"startup requests metadata only and performs no foreground GPU work");
  GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.size()==2,"pending jobs never duplicate");
  for(GpuQualification1961.Probe p:GpuQualification1961.jobs)p.close();GpuQualification1961.jobs.clear();
  GpuModelDefault1990.requestOptional1994();check(GpuQualification1961.jobs.size()==2,"cancelled queued metadata can be rescheduled");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"controlled_transport\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
class GpuCertificates1990 {
 static final Map<String,Evidence> values=new HashMap<String,Evidence>();static final Set<String> rejects=new HashSet<String>();
 static class Evidence{final long gpuNanos;Evidence(long gpu){gpuNanos=gpu;}}
 static Evidence restore(String key){return rejects.contains(key)?null:values.get(key);}
 static boolean rejected(String key){return rejects.contains(key);}
 static void reject(String key,String why){rejects.add(key);values.remove(key);}
 static boolean passed(String key,int count,long pixels,long cpu,long gpu){if(rejected(key))return false;values.put(key,new Evidence(gpu));return true;}
}
class GpuQualification1961 {
 static boolean optionalAllowed1995=true;static boolean optionalBenchmarksAllowed1995(){return optionalAllowed1995;}
 interface Cancellation{boolean cancelled();}interface Probe{void run(Cancellation c);void close();}
 static final List<Probe> jobs=new ArrayList<Probe>();static long bytes;
 static boolean background(){return false;}static boolean exactRejected(String key){return false;}
 static void outcome1984(String reason){}static void timings1984(long cpu,long gpu){}static void exception1993(Throwable failure){}
 static boolean schedule(String key,long size,Probe p){bytes+=size;jobs.add(p);return true;}
}
class GpuNoise1960 {static final int ANALYSIS=2;static boolean supported=true;static boolean supports(int p){return supported;}static int variant(int p,int v){return p+v*9;}static int strongExactPrepare1990(int m,int v){return 51+m+v*3;}}
class GpuStartup1990 {static void invalidateModelDomain(){}}
class CameraTrace1965 {static void event(String k,long id,String value){}}
class StrongNoise1958 {
 static class Model{int value;Model(int value){this.value=value;}}
 static Model preparedSnapshotCpu1961(int[] h,int w,int z,int n,boolean s,float[] e,float[] r){return new Model(w+z+n+Arrays.hashCode(h));}
 static boolean sameModel1961(Model a,Model b){return a.value==b.value;}
 static String modelDifference1991(Model a,Model b){return "controlled one-bit mismatch";}
 static int[][] gpuMaps1960(Model m){return new int[][]{new int[]{m.value}};}
 static void discardResident1961(Model m){GpuAnalysis1961.closed++;}
}
class GpuAnalysis1961 {
 static int calls,closed,fault;
 static String residentExactKey1991(int w,int h,int n,boolean s){return w+":"+h;}
 static StrongNoise1958.Model residentCandidateExact1990(int[] h,int w,int z,int n,boolean s,float[] e,float[] r,int v){
  if(v>0&&fault==2)throw new OutOfMemoryError("controlled");if(v>0&&fault==3)throw new CancellationException("controlled");
  if(w>=3060&&z==258){try{Thread.sleep(v==0?5:fault==4?12:0);}catch(InterruptedException stop){Thread.currentThread().interrupt();throw new CancellationException();}}
  calls++;StrongNoise1958.Model model=StrongNoise1958.preparedSnapshotCpu1961(h,w,z,n,s,e,r);if(v>0&&fault==1)model.value^=1;return model;
 }
}
