package com.hiro.ulike;

import java.util.*;
final class GpuNoiseOptimized19119 {
 static boolean preparedState, supported=true;static String identity="device|candidate=A";
 static boolean prepared(){return preparedState;}
 static boolean prepare(){preparedState=supported;return supported;}
 static String fingerprint(){return identity;}
}
final class GpuCertificates1990 {
 static final Map<String,Object> passes=new HashMap<>();static final Set<String> rejects=new HashSet<>();static int writes;
 static Object restore(String key){return rejects.contains(key)?null:passes.get(key);}
 static boolean passed(String key,int cases,long pixels,long cpu,long gpu){if(rejects.contains(key))return false;passes.put(key,new Object());writes++;return true;}
 static boolean rejected(String key){return rejects.contains(key);}
 static void reject(String key,String reason){rejects.add(key);passes.remove(key);}
}
final class GpuQualification1961 {
 interface Cancellation {boolean cancelled();}
 interface Probe {void run(Cancellation c);void close();}
 static Probe pending;static String outcome;static boolean allowed=true;static int schedules;
 static boolean schedule(String key,long bytes,Probe p){pending=p;schedules++;return true;}
 static boolean optionalStrongBenchmarksAllowed19117(){return allowed;}
 static void outcome1984(String value){outcome=value;}
 static void progress1984(String phase,int done,int total){}
}
final class StrongNoise1958 {
 static final class Model {int width=3060,height=4080;}
 static float[] gpuEvidence1960(Model m){return new float[]{1f};}
 static int[][] gpuMaps1960(Model m){return new int[][]{{1},{2},{3}};}
 static boolean gpuOracleSnapshot1961(int[] source,int[] out,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model model,int[] policy,int[] confidence){
  System.arraycopy(source,0,out,0,source.length);if(confidence!=null)Arrays.fill(confidence,7);return true;
 }
}
final class GpuStrong1960 {
 static int calls,workloadCalls,mutateArgb,mutateConfidence,fail;static boolean workloadMemory,workloadFail,workloadWrongCounts,workloadWrongShape;static long oldTime=100,newTime=89;
 static final class Result {final int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
 static final class Read1971 {final Result result;final String failure;Read1971(Result r,String f){result=r;failure=f;}}
 static Read1971 ephemeralOptimized19119(int[] source,float[] ev,int[][] maps,int[] policy,int[] u,boolean candidate,long[] timing){
  calls++;timing[0]=candidate?newTime:oldTime;if(fail==calls)return null;
  int[] pixels=source.clone(),confidence=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];if(confidence!=null)Arrays.fill(confidence,7);
  if(candidate&&mutateArgb>0)pixels[0]^=1;
  if(candidate&&mutateConfidence>0&&confidence!=null)confidence[0]^=1;
  return new Read1971(new Result(pixels,confidence),null);
 }
 static final class WorkloadResult19119 {
  final long nanos,pixels;final String failure;final int strips,maxInflight,timedModelUploads,tailRows;
  WorkloadResult19119(boolean candidate,int width){if(workloadWrongShape)width=width==3060?4080:3060;nanos=candidate?newTime:oldTime;pixels=12484800L;failure=workloadFail?"readback_failed":null;strips=width==3060?16:12;tailRows=width==3060?240:244;maxInflight=workloadWrongCounts?1:2;timedModelUploads=candidate?1:0;}
 }
 static final class Workload19119 implements AutoCloseable {final int width;Workload19119(int w){width=w;}
  WorkloadResult19119 measure(boolean candidate,GpuQualification1961.Cancellation cancellation){workloadCalls++;return new WorkloadResult19119(candidate,width);}
  public void close(){}
 }
 static Workload19119 prepareWorkload19119(GpuStrongDefault1990.Fixture seed,GpuQualification1961.Cancellation cancellation){return workloadMemory?null:new Workload19119(seed.u[0]);}

}
final class GpuStrongDefault1990 {
 static final class Fixture {final int[] source,u,policy;final StrongNoise1958.Model model=new StrongNoise1958.Model();
  Fixture(int width){u=new int[32];u[0]=width;u[1]=2;u[3]=2;u[5]=2;u[7]=4080;u[8]=4;u[12]=1;source=new int[width*2];Arrays.fill(source,0xff997755);policy=new int[width*4];}}
 static Fixture fixture19112(int id,GpuQualification1961.Cancellation c){return new Fixture(3+id%9);}
 static final class Adaptive1994 {static boolean baseline=true;static boolean faster(){return baseline;}
  static Fixture benchmark19112(int id,GpuQualification1961.Cancellation c){return new Fixture(id==64?3060:4080);}}
 static String difference19112(int[] p,int[] c,int[] e,int[] ec,int offset,int count,GpuQualification1961.Cancellation stop){
  if(p==null||p.length!=count)return "shape";for(int i=0;i<count;i++)if(p[i]!=e[offset+i])return "argb";
  if(!Arrays.equals(c,ec))return "confidence";return null;
 }
}
final class CameraTrace1965 {static void event(String event,long id,String detail){}}
