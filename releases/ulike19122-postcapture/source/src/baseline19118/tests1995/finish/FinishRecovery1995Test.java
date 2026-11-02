package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;

/** Real 28-case recovery and actual queue ownership; injected fault is confined
 * to the inherited 128-entry memory-refusal seam. Pixel implementations stay real. */
public final class FinishRecovery1995Test {
 static int assertions;
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static Field field(String name)throws Exception {Field f=GpuFinishDefault1990.class.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object pending()throws Exception{return field("pendingWide1995").get(null);}
 static int attempts()throws Exception{return field("wideAttempts1995").getInt(null);}
 static void request()throws Exception {GpuFinishDefault1990.requestWide1995();check(pending()!=null,"metadata-only recovery accepted");}
 public static void main(String[] args)throws Exception {
  FinishPolicy1992Test.certificateStore();FinishSchedule1993Test.refuseWide=true;
  check(GpuFinishDefault1990.qualify(GO),"narrow remains independently certified after first wide refusal");
  check(GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.wideReady1993(),"wide failure cannot fabricate wide evidence");
  check(GpuFinishDefault1990.summaryWide1995().contains("最初の失敗"),"wide first refusal visible");
  int narrow=FinishSchedule1993Test.attempts32;
  GpuFinishDefault1990.requestWide1995();check(pending()==null&&attempts()==0,"no optional wide work before first published save");
  GpuQualification1961.startupPause1990(true);GpuQualification1961.publicationSucceeded1995();
  request();Object first=pending();GpuFinishDefault1990.requestWide1995();check(pending()==first,"one metadata owner");
  GpuQualification1961.captureChanged();check(pending()==null&&attempts()==0,"queued capture cancellation closes owner and does not spend retry");
  request();GpuQualification1961.Probe cancelled=(GpuQualification1961.Probe)pending();
  boolean stopped=false;try{cancelled.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){stopped=true;}
  check(stopped&&attempts()==0,"running cancellation does not count as a completed retry");
  GpuQualification1961.captureChanged();check(pending()==null,"cancelled running probe re-arms after close");
  for(int i=0;i<3;i++){
   request();GpuQualification1961.Probe probe=(GpuQualification1961.Probe)pending();probe.run(GO);GpuQualification1961.captureChanged();
   check(attempts()==i+1&&pending()==null,"completed unsuccessful retry counted exactly once");
   check(GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.wideReady1993(),"retry refusal preserves narrow, creates no wide evidence");
   GpuFinishDefault1990.requestWide1995();check(pending()==null,"thirty-second backoff or completed-attempt limit prevents immediate retry");
   field("nextWide1995").setLong(null,0L);
  }
  GpuFinishDefault1990.requestWide1995();check(pending()==null&&attempts()==3,"three completed attempts are a hard bound");
  check(FinishSchedule1993Test.attempts32==narrow,"all independent recovery failures avoid re-running narrow corpus");
  FinishSchedule1993Test.refuseWide=false;
  final int[] checks={0};stopped=false;
  try{GpuFinishDefault1990.qualifyWide1995(new GpuQualification1961.Cancellation(){public boolean cancelled(){return ++checks[0]>=4;}});}catch(CancellationException expected){stopped=true;}
  check(stopped&&GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.wideReady1993(),"mid-corpus cancellation preserves narrow and persists no partial wide corpus");
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"cancelled full-pixel proof drains GPU resources");
  check(GpuFinishDefault1990.qualify(GO),"cached32 qualify still executes missing wide proof");
  GpuCertificates1990.Evidence wide=GpuCertificates1990.restore(GpuFinishDefault1990.wideSuite1993());
  check(wide!=null&&wide.cases==28&&wide.pixels>2500000&&wide.cpuNanos>0&&wide.gpuNanos>0,"recovered wide certificate binds complete finite corpus and actual timings");
  check(FinishSchedule1993Test.attempts32==narrow,"successful wide-only recovery also avoids repeated32 GPU work");
  int before=FinishSchedule1993Test.attempts128;
  check(GpuFinishDefault1990.qualifyWide1995(GO)&&FinishSchedule1993Test.attempts128==before,"completed wide corpus is not needlessly repeated");
  FinishSchedule1993Test.output(false,128);FinishSchedule1993Test.output(true,32);FinishSchedule1993Test.refuseWide=false;
  check(GpuFinishDefault1990.summaryWide1995().contains("128行認定あり")&&GpuFinishDefault1990.summaryWide1995().contains("最初の失敗"),"successful recovery retains historical fault without denying current admission");
  GpuCertificates1990.reject(GpuFinishDefault1990.wideSuite1993(),"controlled completed unequal output");before=FinishSchedule1993Test.attempts128;
  check(!GpuFinishDefault1990.qualifyWide1995(GO)&&FinishSchedule1993Test.attempts128==before,"sticky exact rejection prevents recovery GPU dispatch");
  check(GpuFinishDefault1990.ready()&&!GpuFinishDefault1990.wideReady1993(),"wide exact rejection cannot revoke independent32 certificate");
  field("wideAttempts1995").setInt(null,0);field("nextWide1995").setLong(null,0L);GpuFinishDefault1990.requestWide1995();
  check(pending()==null,"optional request respects sticky wide rejection");
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all real recovery resources retired");
  GpuQualification1961.startupPause1990(false);
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"wide_cases\":28,\"wide_exact_pixels\":"+wide.pixels+",\"partial_wide_failure_preserves32\":true,\"cancel_rearm_verified\":true,\"bounded_metadata_retry_verified\":true,\"actual_jni\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
