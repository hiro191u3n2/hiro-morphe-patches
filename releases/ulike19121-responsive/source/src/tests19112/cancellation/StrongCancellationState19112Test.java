package com.hiro.ulike;
import java.util.concurrent.CancellationException;
public final class StrongCancellationState19112Test {
 static int assertions,cancellations;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static boolean inside(String name){for(StackTraceElement s:Thread.currentThread().getStackTrace())if(s.getMethodName().equals(name))return true;return false;}
 static void reset(){GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.proofs.put(GpuStrongDefault1990.SUITE,new GpuCertificates1990.Evidence(68,1,1,1));GpuStrong1960.submits=0;StrongNoise1958.oracleCalls=0;GpuStrong1960.baselineDelay1995=false;GpuQualification1961.progressThrows=false;GpuQualification1961.progressCalls=0;GpuQualification1961.phase=null;}
 public static void main(String[] args){
  for(final int profile:new int[]{6,7})for(final String phase:new String[]{"fixture19112","difference19112"}){
   reset();GpuQualification1961.Cancellation token=new GpuQualification1961.Cancellation(){int count;public boolean cancelled(){return inside(phase)&&++count==5;}};
   boolean stopped=false;try{GpuStrongDefault1990.Adaptive1994.qualify1998(profile,token,true);}catch(CancellationException expected){stopped=true;}
   check(stopped,"production optional qualifier propagates cancellation "+profile+" "+phase);cancellations++;
   check(GpuCertificates1990.proofs.size()==1&&GpuCertificates1990.rejects.isEmpty(),"partial comparison writes no exact/fast proof or quality rejection");
   check(GpuStrongDefault1990.ready(),"independent standard readiness survives");
   check(phase.equals("fixture19112")?GpuStrong1960.submits==0&&StrongNoise1958.oracleCalls==0:GpuStrong1960.submits>0,"cancelled fixture prevents oracle/GPU launch");
   check(GpuQualification1961.phase.equals(phase.equals("fixture19112")?"strong_cpu_reference":"strong_profile"),"cancellation phase distinguishes CPU prep and GPU comparison");
  }
  for(boolean fault:new boolean[]{false,true}){
   reset();GpuQualification1961.progressThrows=fault;GpuStrong1960.baselineDelay1995=true;
   boolean passed=GpuStrongDefault1990.Adaptive1994.qualify1998(6,new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}},true);
   check(passed,"complete optional proof succeeds with controlled faster candidate");
   check(GpuCertificates1990.proofs.containsKey(GpuStrongDefault1990.Adaptive1998.EXACT)&&GpuCertificates1990.proofs.containsKey(GpuStrongDefault1990.Adaptive1998.FAST),"only full corpus and full speed comparison write both certificates");
   check(StrongNoise1958.oracleCalls==70&&GpuStrong1960.submits==80,"unchanged 70-case corpus and 2-width full speed matrix");
   check(GpuQualification1961.progressCalls==143&&"strong_speed_compare".equals(GpuQualification1961.phase),"bounded per-case diagnostics outside timed calls");
  }
  check(GpuStrongDefault1990.Adaptive1994.measuredWin(100,90)&&!GpuStrongDefault1990.Adaptive1994.measuredWin(100,91),"unchanged 10 percent speed admission");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cancelled_qualifiers\":"+cancellations+",\"partial_proofs_written\":0,\"completed_oracle_calls\":70,\"completed_gpu_submits\":80}");
 }
}
