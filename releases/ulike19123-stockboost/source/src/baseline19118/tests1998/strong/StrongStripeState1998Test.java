package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
/** Controlled owner, certificate, epoch and transport faults; no shader speed claim. */
public final class StrongStripeState1998Test {
 static int assertions;
 static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void reset()throws Exception{
  GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuStrong1960.fault=0;GpuStrong1960.slowCandidate=false;GpuStartup1990.invalidated=false;
  GpuNoise1960.supported=true;GpuNoise1960.warmed=GpuNoise1960.warmed6=GpuNoise1960.warmed7=true;
  GpuQualification1961.exactRejected=false;GpuQualification1961.optionalAllowed1995=true;GpuQualification1961.preFirstIdle19117=false;GpuQualification1961.acceptQueue=true;GpuQualification1961.scheduled=0;GpuQualification1961.pending=null;ProcessingTiming1947.epoch=10;
  for(Class<?> c:new Class<?>[]{GpuStrongDefault1990.Adaptive1994.class,GpuStrongDefault1990.Adaptive1998.class})for(String name:new String[]{"requested","attempted"}){Field f=c.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
  Field f=GpuStrongDefault1990.Adaptive1998.class.getDeclaredField("completedEpoch");f.setAccessible(true);f.setLong(null,Long.MAX_VALUE);
  GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
 }
 static void proof6(){GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1998.EXACT,70,1,1,1);GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1998.FAST,70,1,1,1);}
 static void proof7(){GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1994.EXACT,70,1,1,1);GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1994.FAST,70,1,1,1);}
 static void runQueued(GpuQualification1961.Cancellation c){GpuQualification1961.Probe p=GpuQualification1961.pending;try{p.run(c);}finally{p.close();}}
 static void baseline(){check(GpuStrongDefault1990.ready()&&GpuStrongDefault1990.profile1994()==4,"failed optional keeps standard4");}
 public static void main(String[] args)throws Exception{
  check(!GpuStrongDefault1990.Adaptive1998.EXACT.equals(GpuStrongDefault1990.Adaptive1994.EXACT)&&!GpuStrongDefault1990.Adaptive1998.FAST.equals(GpuStrongDefault1990.Adaptive1994.FAST),"stripe and tile independent certificates");
  reset();check(GpuStrong1960.program1973(6,0)==45,"existing exact stripe program45");baseline();
  GpuCertificates1990.passed(GpuStrongDefault1990.Adaptive1998.EXACT,70,1,1,1);baseline();
  proof6();check(GpuStrongDefault1990.profile1994()==6,"both stripe proofs select6");GpuNoise1960.warmed6=false;int supports=GpuNoise1960.supportCalls;baseline();check(supports==GpuNoise1960.supportCalls,"foreground never compiles candidate");
  check(GpuStrongDefault1990.Adaptive1998.qualify(GO)&&GpuStrongDefault1990.profile1994()==6,"idle cached prepare restores program45");
  int[] u=GpuStrongDefault1990.fixture(0).u;check(GpuStrongDefault1990.accepts1994(u,6),"valid shape adopts independently proven6");GpuQualification1961.exactRejected=true;check(!GpuStrongDefault1990.accepts1994(u,6),"sticky shape rejection remains authoritative");GpuQualification1961.exactRejected=false;
  proof7();check(GpuStrongDefault1990.profile1994()==7,"retained proven7 preferred, no unmeasured6 versus7 replacement");
  reset();for(int fault:new int[]{1,2,3,4}){
   reset();GpuStrong1960.fault=fault;check(!GpuStrongDefault1990.Adaptive1998.qualify(GO),"fault rejects candidate6");baseline();
   check(GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.EXACT)==null&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.FAST)==null,"fault cannot make proof");
   check(GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1998.EXACT)==(fault<=2),"only actual output mismatch is sticky rejection");
   check(!GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1994.EXACT),"fault6 never rejects7");
  }
  reset();GpuStrong1960.slowCandidate=true;check(!GpuStrongDefault1990.Adaptive1998.qualify(GO),"slower stripe candidate not adopted");baseline();check(GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.EXACT)!=null&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1998.FAST)==null,"speed failure preserves only quality");
  reset();GpuStrong1960.fault=5;check(!GpuStrongDefault1990.Adaptive1998.qualify(GO),"broader benchmark finds baseline mismatch");check(!GpuStrongDefault1990.ready()&&GpuStartup1990.invalidated&&GpuCertificates1990.rejected(GpuStrongDefault1990.SUITE),"baseline mismatch invalidates actual baseline");check(!GpuCertificates1990.rejected(GpuStrongDefault1990.Adaptive1998.EXACT),"baseline fault does not falsely reject6");
  reset();GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==0,"no heavy proof before first published save");
  reset();GpuQualification1961.optionalAllowed1995=false;GpuQualification1961.preFirstIdle19117=true;
  GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();
  check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"pre-first idle queues one independent tile8 trial, no stripe delay");
  boolean preFirstCancelled=false;try{runQueued(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){preFirstCancelled=true;}
  check(preFirstCancelled&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)==null&&
      GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST)==null&&GpuStrongDefault1990.profile1994()==4,"first shutter leaves standard route and writes no incomplete tile proof");
  GpuQualification1961.preFirstIdle19117=false;GpuStrongDefault1990.request1994();
  check(GpuQualification1961.scheduled==1,"cancelled first-shot idle work does not restart during capture");
  reset();GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("stripe1998"),"new process queues exactly one stripe6 first");
  boolean canceled=false;try{runQueued(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){canceled=true;}check(canceled,"explicit cancel observed");
  check(!GpuStrongDefault1990.Adaptive1998.completedBeforeCapture(),"canceled proof never unlocks7");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2&&GpuQualification1961.lastKey.contains("stripe1998"),"cancelled6 retries6");
  GpuNoise1960.supported=false;runQueued(GO);GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"completed refusal never immediately launches7");
  ProcessingTiming1947.epoch=9;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"older capture cannot unlock7");
  ProcessingTiming1947.epoch=11;GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==3&&GpuQualification1961.lastKey.contains("layout1994"),"only later capture queues one retained tile7 attempt");runQueued(GO);ProcessingTiming1947.epoch=12;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==3,"completed7 not retried every photo");
  reset();proof6();GpuNoise1960.warmed6=false;GpuStrongDefault1990.request1994();runQueued(GO);check(GpuStrongDefault1990.profile1994()==6,"cached6 prepared without heavy trial");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1,"successful6 still defers7 within same capture");ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuStrongDefault1990.profile1994()==6,"successful6 remains selected after later capture: 4=100,6=40,uncompared7=85 cannot regress to7");
  reset();GpuCertificates1990.reject(GpuStrongDefault1990.Adaptive1998.EXACT,"restored stripe mismatch");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"restored sticky6 rejection permits one independent7 candidate");
  reset();proof7();GpuNoise1960.warmed7=false;GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"existing7 only requires lightweight preparation");runQueued(GO);check(GpuStrongDefault1990.profile1994()==7,"cached7 remains active");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1,"cached7 does not queue competing6");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"profile6_independent_certificates\":true,\"one_heavy_candidate_per_capture\":true,\"existing_profile7_preserved\":true,\"controlled_faults\":true}");
 }
}
