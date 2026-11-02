package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;

/** Controlled transport isolates real optional-family lifecycle and full-suite
 * retry; the retained JNI suite separately proves production pixel equality.
 * .98 keeps this test on the new stripe owner; the outer dispatcher now
 * also honors later-capture tile eligibility. */
public final class StrongRecovery1998Test {
 static int checks;
 static final String EXACT=GpuStrongDefault1990.Adaptive1998.EXACT,FAST=GpuStrongDefault1990.Adaptive1998.FAST;
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void set(String name,boolean v)throws Exception{Field f=GpuStrongDefault1990.Adaptive1998.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,v);}
 static void freshProcess()throws Exception{set("requested",false);set("attempted",false);GpuQualification1961.pending=null;GpuQualification1961.scheduled=0;GpuQualification1961.acceptQueue=true;GpuNoise1960.warmed=false;Thread.interrupted();}
 static void reset()throws Exception{StrongStripeState1998Test.reset();freshProcess();GpuQualification1961.optionalAllowed1995=true;GpuStrong1960.submits=0;GpuStrong1960.baselineDelay1995=true;GpuCertificates1990.interruptExact1995=false;GpuCertificates1990.failFast1995=false;}
 static void execute(boolean expectCancel){GpuQualification1961.Probe p=GpuQualification1961.pending;check(p!=null,"queued owner exists");boolean cancel=false;try{p.run(GO);}catch(CancellationException expected){cancel=true;}finally{p.close();GpuQualification1961.pending=null;}check(cancel==expectCancel,"cancellation outcome matches expected");}
 static void all70(){GpuCertificates1990.Evidence e=GpuCertificates1990.restore(EXACT);check(e!=null&&e.cases==70&&GpuStrong1960.submits>=80,"retry executes unchanged 70-case corpus plus full speed comparisons");}
 public static void main(String[] args)throws Exception{
  reset();GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.Adaptive1998.request();check(GpuQualification1961.scheduled==0,"heavy request blocked before first success");
  boolean deferred=false;try{GpuStrongDefault1990.Adaptive1998.qualify(GO);}catch(CancellationException expected){deferred=true;}
  check(deferred&&GpuStrong1960.submits==0,"direct optional entry also cannot run synthetic pixels before publication");
  GpuCertificates1990.passed(EXACT,70,1,1,1);GpuCertificates1990.passed(FAST,70,1,1,1);GpuNoise1960.warmed=false;
  GpuStrongDefault1990.Adaptive1998.request();check(GpuQualification1961.scheduled==1,"cached exact-and-fast shader preparation admitted pre-first");execute(false);check(GpuStrong1960.submits==0&&GpuStrongDefault1990.profile1994()==6,"cached preparation runs no pixel corpus");
  freshProcess();GpuNoise1960.warmed=false;GpuStrongDefault1990.Adaptive1998.request();GpuCertificates1990.proofs.remove(FAST);GpuQualification1961.optionalAllowed1995=true;
  execute(true);check(GpuStrong1960.submits==0,"cached owner cannot turn heavy when certificate disappears");
  GpuStrongDefault1990.Adaptive1998.request();execute(false);all70();check(GpuCertificates1990.restore(FAST)!=null,"next post-publication request recovers incomplete certificate");
  reset();GpuCertificates1990.interruptExact1995=true;GpuStrongDefault1990.Adaptive1998.request();execute(true);
  check(GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(FAST)==null,"interruption between writes leaves exact only");
  Thread.interrupted();GpuCertificates1990.interruptExact1995=false;GpuStrong1960.submits=0;GpuStrongDefault1990.Adaptive1998.request();execute(false);all70();check(GpuCertificates1990.restore(FAST)!=null,"cancelled exact-only candidate rearms same process");
  reset();GpuCertificates1990.failFast1995=true;GpuStrongDefault1990.Adaptive1998.request();execute(false);all70();check(GpuCertificates1990.restore(FAST)==null,"failed FAST persistence never admits candidate");
  int scheduled=GpuQualification1961.scheduled;GpuStrongDefault1990.Adaptive1998.request();check(GpuQualification1961.scheduled==scheduled,"completed store failure not repeated every photograph");
  freshProcess();GpuCertificates1990.failFast1995=false;GpuStrong1960.submits=0;GpuStrongDefault1990.Adaptive1998.request();execute(false);all70();check(GpuCertificates1990.restore(FAST)!=null,"next process can complete stored exact-only candidate");
  reset();GpuStrong1960.baselineDelay1995=false;GpuStrong1960.slowCandidate=true;GpuStrongDefault1990.Adaptive1998.request();execute(false);all70();
  check(GpuCertificates1990.restore(FAST)==null&&"speed_condition".equals(GpuQualification1961.lastOutcome),"actual slow result remains speed failure");scheduled=GpuQualification1961.scheduled;GpuStrongDefault1990.Adaptive1998.request();check(GpuQualification1961.scheduled==scheduled,"actual slow completed proof runs once per process");
  reset();GpuStrong1960.fault=1;GpuStrongDefault1990.Adaptive1998.request();execute(false);check(GpuCertificates1990.rejected(EXACT)&&GpuStrongDefault1990.profile1994()==4,"quality rejection remains authoritative");
  check(!GpuQualification1961.scheduledUnderAdaptiveLock1995,"queue never entered while holding Adaptive class lock");
  Constructor<?>[] constructors=Class.forName("com.hiro.ulike.GpuStrongDefault1990$Adaptive1998$1").getDeclaredConstructors();check(constructors.length==1&&constructors[0].getParameterTypes().length==0,"anonymous Strong owner ABI remains no-argument");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"controlled_lifecycle\":true,\"physical_android_tested\":false}");
 }
}
