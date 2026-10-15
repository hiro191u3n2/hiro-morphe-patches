package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.*;

/** Production ownership/admission/CPU math; controlled GPU transport does not
 * supply any performance result or fabricate pixel equality. */
public final class ModelRecovery1991Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static int queued(String prefix)throws Exception {
  int count=0;Object lock=field(GpuQualification1961.class,"LOCK").get(null);
  synchronized(lock){for(Object job:(Iterable<?>)field(GpuQualification1961.class,"QUEUED").get(null))
   if(((String)field(job.getClass(),"key").get(job)).startsWith(prefix))count++;
  }return count;
 }
 public static void main(String[] args)throws Exception {
  android.os.Build.FINGERPRINT="model-recovery1991-host";GpuNoise1960.enabled=true;
  Context context=new Context();GpuQualification1961.initialize(context);GpuCertificates1990.initialize(context);
  field(StrongNoise1958.class,"nativeState").setInt(null,-1);
  GpuModelDefault1990.Fixture f=GpuModelDefault1990.fixture(0);
  String exact=GpuAnalysis1961.residentExactKey1991(f.width,f.height,f.noise,f.shadows);
  String legacy=GpuAnalysis1961.residentKey(f.width,f.height,f.noise,f.shadows);
  check(!exact.equals(legacy),"corrected model never reinterprets legacy shader evidence");
  check(!exact.equals(GpuAnalysis1961.residentExactKey1991(f.width+1,f.height,f.noise,f.shadows)),"shape changes key");
  check(!exact.equals(GpuAnalysis1961.residentExactKey1991(f.width,f.height,f.noise+1,f.shadows)),"noise changes key");
  check(!exact.equals(GpuAnalysis1961.residentExactKey1991(f.width,f.height,f.noise,!f.shadows)),"shadow setting changes key");
  StrongNoise1958.Model reference=StrongNoise1958.preparedSnapshotCpu1961(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime);
  StrongNoise1958.Model same=StrongNoise1958.preparedSnapshotCpu1961(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime);
  check(StrongNoise1958.sameModel1961(reference,same),"independent CPU models identical");
  check("equal".equals(StrongNoise1958.modelDifference1991(reference,same)),"no fabricated model discrepancy");
  int[] half=StrongNoise1958.gpuMaps1960(same)[0];half[7]^=1;
  check(!StrongNoise1958.sameModel1961(reference,same),"one-bit model change rejected");
  check(StrongNoise1958.modelDifference1991(reference,same).startsWith("map=0 index=7"),"first exact map difference explained");half[7]^=1;
  float[] ev=(float[])field(StrongNoise1958.Model.class,"evidence").get(same);ev[17]=Float.intBitsToFloat(Float.floatToRawIntBits(ev[17])+1);
  check(StrongNoise1958.modelDifference1991(reference,same).startsWith("evidence=0 index=17"),"one-ULP evidence difference explained");
  int[] selection=new int[2];int opened=GpuNoise1960.openAttempts;
  GpuQualification1961.qualified(legacy,100000000L,1L,0);
  check(GpuAnalysis1961.residentIfQualified1988(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,selection)==null,"legacy proof cannot admit corrected model");
  check(GpuNoise1960.openAttempts==opened&&selection[1]==PipelineDetail1988.PROOF_MISSING,"missing exact proof performs no speculative GPU work");
  check(!GpuModelDefault1990.ready(),"transient startup absence is not ready");
  GpuAnalysis1961.scheduleResident(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,reference,100000000L);
  check(queued(exact)==1,"transient preflight absence may collect exact per-shape proof");
  check(GpuQualification1961.restore(exact)==null,"merely queued proof grants no GPU permission");
  check(GpuQualification1961.retainedBytes()>0,"model snapshot reserved and accounted");
  GpuQualification1961.captureChanged();check(GpuQualification1961.retainedBytes()==0,"capture cancellation releases model ownership");
  check(GpuCertificates1990.passed(GpuModelDefault1990.SUITE,32,1000,100,200),"finite CPU/GPU match evidence accepts real timing without speed claim");
  GpuAnalysis1961.scheduleResident(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,reference,100000000L);
  check(queued(exact)==0,"valid standard model does not retain redundant photo proof");
  GpuCertificates1990.reject(GpuModelDefault1990.SUITE,"controlled model discrepancy");
  check(!GpuModelDefault1990.ready(),"mismatch revokes cached pass immediately");
  GpuAnalysis1961.scheduleResident(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,reference,100000000L);
  check(queued(exact)==0&&GpuQualification1961.retainedBytes()==0,"finite suite mismatch cannot be bypassed by per-shape retry");
  check(GpuAnalysis1961.residentIfQualified1988(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,selection)==null&&selection[1]==PipelineDetail1988.QUALIFICATION_BLOCKED,"mismatch reports blocked, not missing proof");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"device_speedup_verified\":false,\"transient_model_recovery_verified\":true,\"exact_mismatch_block_preserved\":true}");
 }
}
